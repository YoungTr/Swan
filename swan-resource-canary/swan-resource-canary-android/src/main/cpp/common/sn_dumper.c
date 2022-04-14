//
// Created by YoungTr on 2022/4/14.
//

#include <log.h>
#include <pthread.h>
#include "sn_dumper.h"
#include "sn_dl.h"
#include "sn_errno.h"

static pthread_once_t once_control = PTHREAD_ONCE_INIT;
static int sn_dump_symbols_loaded = 0;
static int sn_dump_symbols_status = SWAN_ERRNO_NOTFND;
int sn_android_api_level = 0;

static sn_util_libart_dbg_suspend_t sn_trace_libart_dbg_suspend = NULL;
static sn_util_libart_dbg_resume_t sn_trace_libart_dbg_resume = NULL;

void init_api() {
    sn_android_api_level = android_get_device_api_level();
    LOGD("android_api_ = %d", sn_android_api_level);
}

int initialize() {
    pthread_once(&once_control, init_api);
    sn_dl_t *libart = NULL;

    // only init once
    if (sn_dump_symbols_loaded) return sn_dump_symbols_status;

    sn_dump_symbols_loaded = 1;

    if (sn_android_api_level >= __ANDROID_API_R__)
        libart = sn_dl_create(SWAN_UTIL_LIBART_APEX_30);     // android 11
    else if (sn_android_api_level == __ANDROID_API_Q__)
        libart = sn_dl_create(SWAN_UTIL_LIBART_APEX_29);     // android 10

    if (NULL == libart && NULL == (libart = sn_dl_create(SWAN_UTIL_LIBART))) goto end;

    if (NULL == (sn_trace_libart_dbg_suspend = sn_dl_sym(libart, SWAN_UTIL_LIBART_DBG_SUSPEND)))
        goto end;
    if (NULL == (sn_trace_libart_dbg_resume = sn_dl_sym(libart, SWAN_UTIL_LIBART_DBG_RESUME)))
        goto end;

    sn_dump_symbols_status = 0;
    end:
    if (NULL != libart) sn_dl_destroy(&libart);
    return sn_dump_symbols_status;
}

void suspended() {
    sn_trace_libart_dbg_suspend();
}

void resumed() {
    sn_trace_libart_dbg_resume();
}
