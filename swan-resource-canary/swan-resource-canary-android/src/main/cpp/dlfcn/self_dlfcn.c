//
// Created by YoungTr on 2022/4/12.
//

#include <dlfcn.h>
#include "self_dlfcn.h"
#include <pthread.h>
#include "log.h"
#include "scheck.h"

static pthread_once_t once_control = PTHREAD_ONCE_INIT;
int android_api_;
int init_done = INIT_DLF_FAILED;

void init_api1() {
    android_api_ = android_get_device_api_level();
    LOGD("android_api_ = %d", android_api_);
}


void *self_dlopen(const char *lib_name, int flag) {
    pthread_once(&once_control, init_api1);
    if (android_api_ < __ANDROID_API_N__) {
        return dlopen(lib_name, flag);
    }
    if (android_api_ >= __ANDROID_API_O__) {
        void *handler = dlopen(UTIL_LIBDL, RTLD_NOW);
        SCHECKP(handler)
        loader_dlopen_fn_t loader_dlopen_fn = NULL;
        loader_dlopen_fn = dlsym(handler, UTIL_LIBDL_LOADER_DLOPEN);
        SCHECKP(loader_dlopen_fn)
        if (android_api_ < __ANDROID_API_Q__) {
            return loader_dlopen_fn(lib_name, flag, (void *) dlerror);
        }
    }
    return NULL;
}

void *self_dlsym(void *handler, const char *name) {
    return dlsym(handler, name);
}

void self_dlclose(void *handler) {
    dlclose(handler);
}

int initialized() {
    if (init_done) return 1;
    void *handler;
    if ((handler = self_dlopen(UTIL_LIBART, RTLD_NOW)) == NULL) {
        LOGD("can't find %s", UTIL_LIBART);
        return INIT_DLF_FAILED;
    }
    if ((libart_dbg_suspend = self_dlsym(handler, UTIL_LIBART_DBG_SUSPEND)) == NULL) {
        LOGD("can't find %s", UTIL_LIBART_DBG_SUSPEND);
        return INIT_DLF_FAILED;
    }

    if ((libart_dbg_resume = self_dlsym(handler, UTIL_LIBART_DBG_RESUME)) == NULL) {
        LOGD("can't find %s", UTIL_LIBART_DBG_RESUME);
        return INIT_DLF_FAILED;
    }
    init_done = INIT_DLF_SUCCESS;
    return init_done;
}

void suspend() {
    libart_dbg_suspend();
}

void resume() {
    libart_dbg_resume();
}