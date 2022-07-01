#include <jni.h>
#include <string>
#include <sys/auxv.h>
#include <link.h>
#include "sHook.h"
#include "log.h"
#include "swan_link.h"

extern __attribute((weak)) unsigned long int getauxval(unsigned long int);
extern __attribute((weak)) int dl_iterate_phdr(int (*)(struct dl_phdr_info *, size_t, void *), void *);


extern "C" JNIEXPORT jstring JNICALL
Java_com_bomber_swan_sample_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT void JNICALL
Java_sample_com_bomber_swan_hook_HookActivity_nativeHook(JNIEnv *env, jobject thiz, jstring path) {
    uintptr_t base = (uintptr_t)getauxval(AT_BASE);
    LOGD("link base: %x", base);


//    hook_fwrite(env->GetStringUTFChars(path, nullptr));
}
extern "C"
JNIEXPORT void JNICALL
Java_sample_com_bomber_swan_hook_HookActivity_nativeWrite(JNIEnv *env, jobject thiz,
                                                          jstring file_path,
                                                          jstring content) {

    swan_iterate_phdr();


//    const char *path = env->GetStringUTFChars(file_path, nullptr);
//    const char *text = env->GetStringUTFChars(content, nullptr);
//    FILE *fp = NULL;
//    if ((fp = fopen(path, "w")) == NULL) {
//        LOGE("file cannot open");
//        return;
//    }
//    //写入数据
//    fwrite(text, strlen(text), 1, fp);
//    if (fclose(fp) != 0) {
//        LOGE("file cannot be closed");
//        return;
//    }
}