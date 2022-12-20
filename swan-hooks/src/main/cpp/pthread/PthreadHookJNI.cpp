//
// Created by YoungTr on 2022/12/17.
//
#include <jni.h>
#include "../common/Log.h"
#include "PthreadHook.h"
#include "ThreadTrace.h"
#include "PthreadHook.h"

using namespace pthread_hook;
using namespace thread_trace;

extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_hooks_pthread_PthreadHook_addHookThreadNameNative(JNIEnv *env, jobject thiz,
                                                                       jobjectArray thread_names) {
    jsize size = env->GetArrayLength(thread_names);
    for (int i = 0; i < size; i++) {
        auto jregx = (jstring) (env->GetObjectArrayElement(thread_names, i));
        const char *regex = env->GetStringUTFChars(jregx, nullptr);
        add_hook_thread_name(regex);
        env->ReleaseStringUTFChars(jregx, regex);
    }

}
extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_hooks_pthread_PthreadHook_enableQuickenNative(JNIEnv *env, jobject thiz,
                                                                   jboolean enable_quick) {
    enable_quicken_unwind(enable_quick);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_hooks_pthread_PthreadHook_enableLoggerNative(JNIEnv *env, jobject thiz,
                                                                  jboolean enable_logger) {
    enable_hook_logger(enable_logger);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_hooks_pthread_PthreadHook_enableTracePthreadReleaseNative(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jboolean enable_trace) {
    enable_trace_pthread_release(enable_trace);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_hooks_pthread_PthreadHook_installHooksNative(JNIEnv *env, jobject thiz,
                                                                  jboolean enable_debug) {
    InstallHooks(enable_debug);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_hooks_pthread_PthreadHook_dumpNative(JNIEnv *env, jobject thiz,
                                                          jstring jpath) {
    if (jpath) {
        const char *path = env->GetStringUTFChars(jpath, nullptr);
        pthread_dump_json(path);
        env->ReleaseStringUTFChars(jpath, path);
    }
}