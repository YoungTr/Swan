//
// Created by YoungTr on 2022/4/12.
//

#ifndef SWAN_SELF_DLFCN_H
#define SWAN_SELF_DLFCN_H

#ifdef __cplusplus
extern "C" {
#endif

#define UTIL_LIBART  "libart.so"
#define UTIL_LIBART_DBG_SUSPEND      "_ZN3art3Dbg9SuspendVMEv"
#define UTIL_LIBART_DBG_RESUME       "_ZN3art3Dbg8ResumeVMEv"

typedef void (*libart_dbg_suspend_t)(void);

typedef void (*libart_dbg_resume_t)(void);

static libart_dbg_suspend_t libart_dbg_suspend = NULL;

static libart_dbg_resume_t libart_dbg_resume = NULL;


void initialize();

void *self_dlopen(const char *);

void *self_dlsym(void *handler, const char *);


void self_dlclose(void *handler);

#ifdef __cplusplus
}
#endif

#endif //SWAN_SELF_DLFCN_H
