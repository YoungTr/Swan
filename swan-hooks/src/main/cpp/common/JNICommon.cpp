//
// Created by YoungTr on 2022/12/13.
//

#include <mutex>
#include "JNICommon.h"
#include "ScopedCleaner.h"
#include "Log.h"
#include "PthreadExt.h"
#include "Backtrace.h"
#include "QuickenUnwinder.h"
#include "xhook.h"

#define TAG "Swan.JNICommon"


#ifdef __cplusplus

extern "C" {
#endif

JavaVM *m_java_vm;
static volatile bool s_prehook_initialized = false;
static std::mutex s_prehook_init_mutex;
static volatile bool s_finalhook_initialized = false;
static std::mutex s_finalhook_init_mute;
jclass m_class_HookManager;
jmethodID m_method_getStack;


JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGD(TAG, "JNI OnLoad...");
    m_java_vm = vm;
    return JNI_VERSION_1_6;
}

static jclass FindClass(JNIEnv *env, const char *desc, bool ret_global_ref = false) {
    jclass clazz = env->FindClass(desc);
    if (nullptr == clazz) {
        env->ExceptionClear();
        LOGE(TAG, "Cannot find class: %s", desc);
    }
    if (ret_global_ref) {
        clazz = reinterpret_cast<jclass>(env->NewWeakGlobalRef(clazz));
        if (nullptr == clazz) {
            LOGE(TAG, "Cannot create global ref for class: %s.", desc);
        }
    }
    return clazz;
}

static jmethodID GetStaticMethodId(JNIEnv *env, jclass clazz, const char *name, const char *sig) {
    jmethodID mid = env->GetStaticMethodID(clazz, name, sig);
    if (nullptr == mid) {
        env->ExceptionClear();
    }
    return mid;
}


JNIEXPORT jboolean JNICALL
Java_com_bomber_swan_hooks_HookManager_doPreHookInitializeNative(JNIEnv *env, jobject thiz,
                                                                 jboolean enable_debug) {
    std::lock_guard<std::mutex> prehookInitLock(s_prehook_init_mutex);

    if (s_prehook_initialized) {
        LOGE(TAG, "doPreHookInitializeNative was already called.");
        return true;
    }

    m_class_HookManager = FindClass(env, "com/bomber/swan/hooks/HookManager", true);
    if (nullptr == m_class_HookManager) {
        LOGE(TAG, "Not find class HookManager");
        return false;
    }
    m_class_HookManager = reinterpret_cast<jclass>(env->NewGlobalRef(m_class_HookManager));
    auto jHookMgrCleaner = swan::MakeScopedCleaner([env]() {
        LOGD(TAG, "MakeScopedCleaner HookManager");
        if (nullptr != m_class_HookManager) {
            env->DeleteGlobalRef(m_class_HookManager);
            m_class_HookManager = nullptr;
        }
    });

    m_method_getStack = GetStaticMethodId(env, m_class_HookManager, "getStack",
                                          "()Ljava/lang/String;");
    if (nullptr == m_method_getStack) {
        LOGE(TAG, "Not find method getStack");
        return false;
    }

    auto getStackMethodCleaner = swan::MakeScopedCleaner([]() {
        LOGD(TAG, "MakeScopedCleaner GetStack");
        m_method_getStack = nullptr;
    });


    getStackMethodCleaner.Omit();
    jHookMgrCleaner.Omit();


    s_prehook_initialized = true;
    return true;
}
extern "C"
JNIEXPORT void JNICALL

Java_com_bomber_swan_hooks_HookManager_doFinishInitializeNative(JNIEnv *env, jobject thiz,
                                                                jboolean enable_debug) {
    std::lock_guard<std::mutex> finalInitLock(s_finalhook_init_mute);
    if (s_finalhook_initialized) {
        LOGE(TAG, "doFinalInitializeNative was already called.");
        return;
    }

    wechat_backtrace::notify_maps_changed();

    xhook_enable_debug(enable_debug ? 1 : 0);
    xhook_enable_sigsegv_protection(enable_debug ? 1 : 0);

    int ret = xhook_refresh(0);
    if (0 != ret) {
        LOGE(TAG, "Fail to call xhook_refresh, ret: %d", ret);
    }
    s_finalhook_initialized = true;
}

#ifdef __cplusplus
}
#endif