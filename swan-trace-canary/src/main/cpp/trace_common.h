//
// Created by YoungTr on 2022/6/12.
//

#ifndef SWAN_TRACE_COMMON_H
#define SWAN_TRACE_COMMON_H

#include <jni.h>
#include <time.h>

#define ERRNO_SYS     ((0 != errno) ? errno : 1004)
#define ERRNO_INVAL    1002


extern int common_api_level;
extern JavaVM *common_vm;

extern pid_t common_process_id;

#define JNI_IGNORE_PENDING_EXCEPTION()                 \
    do {                                                  \
        if((*env)->ExceptionCheck(env))                   \
        {                                                 \
            (*env)->ExceptionClear(env);                  \
        }                                                 \
    } while(0)

#define UTIL_MIN(a,b) ({         \
            __typeof__ (a) _a = (a); \
            __typeof__ (b) _b = (b); \
            _a < _b ? _a : _b; })

#ifdef __cplusplus
extern "C" {
#endif



int common_init(int api_level);

void set_common_vm(JavaVM *vm);

int util_atoi(const char *str, int *i);

char *util_gets(char *s, size_t size, int fd);

int util_read_file_line(const char *path, char *buf, size_t len);

int util_get_process_thread_name(const char *path, char *buf, size_t len);

void util_get_thread_name(pid_t tid, char *buf, size_t len);

#ifdef __cplusplus
}
#endif

#endif //SWAN_TRACE_COMMON_H
