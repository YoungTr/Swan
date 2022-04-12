//
// Created by YoungTr on 2022/4/12.
//

#include <dlfcn.h>
#include "self_dlfcn.h"


void *self_dlopen(const char *path) {
    return dlopen(path, RTLD_NOW);
}

void *self_dlsym(void *handler, const char *name) {
    return dlsym(handler, name);
}

void self_dlclose(void *handler) {
    dlclose(handler);
}

void initialize() {
    void *handler = self_dlopen(UTIL_LIBART);
    libart_dbg_suspend = self_dlsym(handler, UTIL_LIBART_DBG_SUSPEND);
    libart_dbg_resume = self_dlsym(handler, UTIL_LIBART_DBG_RESUME);
}