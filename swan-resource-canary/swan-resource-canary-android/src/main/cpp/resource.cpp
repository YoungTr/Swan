#include <jni.h>
#include <string>
#include <dlfcn.h>
#include <sys/prctl.h>
#include "log.h"
#include <wait.h>
#include "xdl.h"

#include <unistd.h>
#include <swan_common.h>
#include "swan_dumper.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_bomber_swan_resource_NativeLib_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT int JNICALL
Java_com_bomber_swan_resource_matrix_dumper_ForkJvmHeapDumperKt_resumeAndWait(JNIEnv *env,
                                                                            jclass clazz,
                                                                            jint pid) {
   return swan_dump_resumed(pid);
}
extern "C"
JNIEXPORT int JNICALL
Java_com_bomber_swan_resource_matrix_dumper_ForkJvmHeapDumperKt_suspendAndFork(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jint wait) {

    return swan_dump_suspend(wait);

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_bomber_swan_resource_matrix_dumper_ForkJvmHeapDumperKt_initializedNative(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jint api_level) {

    int r;
    if (0 != (r = swan_common_init(api_level))) return r;
    if (0 != (r = swan_dump_init())) return r;
    return 0;

}