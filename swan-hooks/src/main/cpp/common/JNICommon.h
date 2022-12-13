//
// Created by YoungTr on 2022/12/13.
//

#ifndef SWAN_JNICOMMON_H
#define SWAN_JNICOMMON_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

extern JavaVM *m_java_vm;

extern jclass m_class_HookManager;
extern jmethodID m_method_getStack;

#ifdef __cplusplus
}
#endif

#endif //SWAN_JNICOMMON_H
