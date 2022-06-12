//
// Created by YoungTr on 2022/6/12.
//

#ifndef SWAN_TRACE_COMMON_H
#define SWAN_TRACE_COMMON_H

#include <jni.h>
#include <time.h>

extern int common_api_level;
extern JavaVM *common_vm;

#define JNI_IGNORE_PENDING_EXCEPTION()                 \
    do {                                                  \
        if((*env)->ExceptionCheck(env))                   \
        {                                                 \
            (*env)->ExceptionClear(env);                  \
        }                                                 \
    } while(0)

#ifdef __cplusplus
extern "C" {
#endif



int common_init(int api_level);

void set_common_vm(JavaVM *vm);

#ifdef __cplusplus
}
#endif

#endif //SWAN_TRACE_COMMON_H
