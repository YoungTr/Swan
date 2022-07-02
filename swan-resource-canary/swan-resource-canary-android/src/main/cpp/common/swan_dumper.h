//
// Created by YoungTr on 2022/4/14.
//

#ifndef SWAN_SWAN_DUMPER_H
#define SWAN_SWAN_DUMPER_H

#ifdef __cplusplus
extern "C" {
#endif

#define SWAN_DUMP_LIB_ART ("libart.so")

#define SWAN_UTIL_LIBART_DBG_SUSPEND      "_ZN3art3Dbg9SuspendVMEv"
#define SWAN_UTIL_LIBART_DBG_RESUME       "_ZN3art3Dbg8ResumeVMEv"

typedef void  (*swan_libart_suspend)(void);

typedef void  (*swan_libart_resumed)(void);

int swan_dump_init();

pid_t swan_dump_suspend(int wait);

int swan_dump_resumed(pid_t pid);


#ifdef __cplusplus
}
#endif

#endif //SWAN_SWAN_DUMPER_H
