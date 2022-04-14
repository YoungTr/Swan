//
// Created by YoungTr on 2022/4/12.
//

#ifndef SWAN_SELF_DLFCN_H
#define SWAN_SELF_DLFCN_H

#ifdef __cplusplus
extern "C" {
#endif

#define UTIL_LIBDL "libdl.so"
#define UTIL_LIBDL_LOADER_DLOPEN "__loader_dlopen"
#define UTIL_LIBART "/system/lib/libart.so"
#define UTIL_LIBART_DBG_SUSPEND "_ZN3art3Dbg9SuspendVMEv"
#define UTIL_LIBART_DBG_RESUME "_ZN3art3Dbg8ResumeVMEv"

#define INIT_DLF_SUCCESS  1
#define INIT_DLF_FAILED  0

typedef void (*libart_dbg_suspend_t)(void);

typedef void (*libart_dbg_resume_t)(void);

typedef void *(*loader_dlopen_fn_t)(const char *filename, int flag,
                                    void *address);


static libart_dbg_suspend_t libart_dbg_suspend = NULL;

static libart_dbg_resume_t libart_dbg_resume = NULL;


int initialized();

void *self_dlopen(const char *, int);

void *self_dlsym(void *handler, const char *);


void self_dlclose(void *handler);

void suspend();

void resume();

#ifdef __cplusplus
}
#endif

#endif //SWAN_SELF_DLFCN_H
