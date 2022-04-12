#include <jni.h>
#include <string>
#include "dlfcn/self_dlfcn.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_bomber_swan_resource_NativeLib_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_resource_matrix_dump_ForkJvmHeapDumperKt_resumeAndWait(JNIEnv *env,
                                                                            jclass clazz,
                                                                            jint pid) {
    libart_dbg_resume();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_resource_matrix_dump_ForkJvmHeapDumperKt_suspendAndFork(JNIEnv *env,
                                                                             jclass clazz,jlong wait) {
    libart_dbg_suspend();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_resource_matrix_dump_ForkJvmHeapDumperKt_init(JNIEnv *env, jclass clazz) {
    initialize();
}