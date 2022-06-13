//
// Created by YoungTr on 2022/6/10.
//

#ifndef SWAN_SIGNAL_HANDLER_H
#define SWAN_SIGNAL_HANDLER_H

#include <jni.h>


#ifdef __cplusplus
extern "C" {
#endif


int sa_signal_init(JNIEnv *env);

#ifdef __cplusplus
}
#endif

#endif //SWAN_SIGNAL_HANDLER_H
