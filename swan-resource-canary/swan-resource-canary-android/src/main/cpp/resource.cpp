#include <jni.h>
#include <string>
#include <dlfcn.h>
#include "sn_dumper.h"

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
    resumed();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bomber_swan_resource_matrix_dump_ForkJvmHeapDumperKt_suspendAndFork(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong wait) {
    suspended();
}

extern "C"
JNIEXPORT int JNICALL
Java_com_bomber_swan_resource_matrix_dump_ForkJvmHeapDumperKt_initializedNative(JNIEnv *env,
                                                                                jclass clazz) {
    return initialize();
}