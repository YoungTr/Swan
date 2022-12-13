//
// Created by YoungTr on 2022/12/13.
//

#include <mutex>
#include "JNICommon.h"
#include "Log.h"

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


JNIEXPORT jboolean JNICALL
Java_com_swan_bomber_hooks_HookManager_doPreHookInitializeNative(JNIEnv *env, jobject thiz,
                                                                 jboolean enable_debug) {
    std::lock_guard<std::mutex> prehookInitLock(s_prehook_init_mutex);

    if (s_prehook_initialized) {
        LOGE(TAG, "doPreHookInitializeNative was already called.");
        return true;
    }

    m_class_HookManager = FindClass(env, "com/swan/bomber/hooks/HookManager", true);
    if (nullptr == m_class_HookManager) {
        return false;
    }
    m_class_HookManager = reinterpret_cast<jclass>(env->NewGlobalRef(m_class_HookManager));



    s_prehook_initialized = true;
    return true;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_swan_bomber_hooks_HookManager_doFinishInitializeNative(JNIEnv *env, jobject thiz,
                                                                jboolean enable_debug) {

}

#ifdef __cplusplus
}
#endif