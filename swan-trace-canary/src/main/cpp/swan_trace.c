//
// Created by YoungTr on 2022/6/10.
//


#include "swan_trace.h"
#include "log.h"

static struct StacktraceJNI {
    jclass AnrDetective;
    jclass ThreadPriorityDetective;
    jclass TouchEventLagTracer;
    jmethodID AnrDetector_onANRDumped;
    jmethodID AnrDetector_onANRDumpTrace;
    jmethodID AnrDetector_onPrintTrace;

    jmethodID AnrDetector_onNativeBacktraceDumped;
} gJ;

static void
nativeInitSignalAnrDetective(JNIEnv *env, jclass cls, jstring anrTracePath, jstring printTracePath) {

}

static void
nativeFreeSignalAnrDetective(JNIEnv *env) {

}

static void
nativePrintTrace(JNIEnv *env) {

}

static const JNINativeMethod ANR_METHODS[] = {
        {"nativeInitSignalAnrDetective", "(Ljava/lang/String;Ljava/lang/String;)V",
                                                (void *) nativeInitSignalAnrDetective},
        {"nativeFreeSignalAnrDetective", "()V", (void *) nativeFreeSignalAnrDetective},
        {"nativePrintTrace",             "()V", (void *) nativePrintTrace}
};


JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {

    LOGD("jni on load");

    JNIEnv *env;
    jclass cls;

    if (NULL == vm) return JNI_ERR;

    if (JNI_OK != (*vm)->GetEnv(vm, (void **) &env, SA_JNI_VERSION)) return JNI_ERR;
    if (NULL == env || NULL == *env) return -1;
    if (NULL == (cls = (*env)->FindClass(env, SA_JNI_CLASS_NAME))) return JNI_ERR;
    gJ.AnrDetective = (*env)->NewGlobalRef(env, cls);
    gJ.AnrDetector_onANRDumped = (*env)->GetStaticMethodID(env, cls, "onANRDumped", "()V");
    gJ.AnrDetector_onANRDumpTrace = (*env)->GetStaticMethodID(env, cls, "onANRDumpTrace", "()V");
    gJ.AnrDetector_onPrintTrace = (*env)->GetStaticMethodID(env, cls, "onANRDumpTrace", "()V");
    gJ.AnrDetector_onNativeBacktraceDumped = (*env)->GetStaticMethodID(env, cls, "onNativeBacktraceDumped", "()V");
    if ((*env)->RegisterNatives(env, cls,ANR_METHODS, sizeof(ANR_METHODS)/ sizeof (ANR_METHODS[0]))) return JNI_ERR;

    (*env)->DeleteLocalRef(env, cls);

    return SA_JNI_VERSION;
}