#include <jni.h>
#include <string>
#include <dlfcn.h>
#include <sys/prctl.h>
#include "log.h"
#include <wait.h>
#include "xdl.h"

#include <unistd.h>
#include "sn_dumper.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_bomber_swan_resource_NativeLib_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT int JNICALL
Java_com_bomber_swan_resource_matrix_dump_ForkJvmHeapDumperKt_resumeAndWait(JNIEnv *env,
                                                                            jclass clazz,
                                                                            jint pid) {
    int status;
    if (waitpid(pid, &status, 0) == -1) {
        LOGE("wait pid %d error", pid);
        return -2;
    }

    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    } else {
        LOGE("Fork process exited unexpectedly.");
        return -2;
    }
}
extern "C"
JNIEXPORT int JNICALL
Java_com_bomber_swan_resource_matrix_dump_ForkJvmHeapDumperKt_suspendAndFork(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong wait) {
    suspended();
    pid_t pid;
    pid = fork();
    if (pid == 0) {
        // child
        alarm(wait);
        prctl(PR_SET_NAME, "forked-dump-process");
    } else {
        // parent
        resumed();
    }

    return pid;

}

extern "C"
JNIEXPORT int JNICALL
Java_com_bomber_swan_resource_matrix_dump_ForkJvmHeapDumperKt_initializedNative(JNIEnv *env,
                                                                                jclass clazz) {
    return initialize();
}