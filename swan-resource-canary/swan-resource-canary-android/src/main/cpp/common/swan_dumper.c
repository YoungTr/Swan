//
// Created by YoungTr on 2022/4/14.
//

#include <tls.h>
#include <inttypes.h>
#include <unistd.h>
#include <sys/prctl.h>
#include <sys/wait.h>
#include <malloc.h>
#include "log.h"
#include "xdl.h"
#include "swan_errno.h"
#include "swan_dumper.h"
#include "swan_common.h"

static int swan_dump_symbols_loaded = 0;
static int swan_dump_symbols_status = SWAN_ERRNO_NOTFND;

static swan_libart_suspend dump_libart_suspend = NULL;
static swan_libart_resumed dump_libart_resumed = NULL;

static swan_libart_ssa_constructor_fnc_ dump_ssa_constructor = NULL;
static swan_libart_ssa_destructor_fnc_ dump_ssa_destructor = NULL;
static swan_libart_sgc_constructor_fnc_ dump_sgc_constructor = NULL;
static swan_libart_sgc_destructor_fnc_ dump_sgc_destructor = NULL;
static swan_libart_mutator_lock_ptr_ dump_mutator_lock_ptr = NULL;
static swan_libart_exclusive_lock_fnc_ dump_exclusive_lock = NULL;
static swan_libart_exclusive_unlock_fnc_ dump_exclusive_unlock = NULL;

static void *swan_libart_ssa_instance;
static void *swan_libart_sgc_instance;


int swan_dump_init() {

    LOGD("app level: %d", swan_common_api_level);
    // only init once
    if (swan_dump_symbols_loaded) return swan_dump_symbols_status;
    swan_dump_symbols_loaded = 1;

    void *handler;

    if (NULL == (handler = xdl_open(SWAN_DUMP_LIB_ART, XDL_DEFAULT))) goto end;

    LOGD(">>> xdl_open(%s) : handle %" PRIxPTR, "libart.so", (uintptr_t) handler);

    if (swan_common_api_level < __ANDROID_API_R__) { // < android 11
        if (NULL == (dump_libart_resumed = xdl_sym(handler, SWAN_DUMP_LIBART_DBG_RESUME, NULL))) goto end;
        if (NULL == (dump_libart_suspend = xdl_sym(handler, SWAN_DUMP_LIBART_DBG_SUSPEND, NULL))) goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", SWAN_DUMP_LIBART_DBG_RESUME, (uintptr_t) dump_libart_resumed);
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", SWAN_DUMP_LIBART_DBG_SUSPEND, (uintptr_t) dump_libart_suspend);
    } else if (swan_common_api_level <= __ANDROID_API_S__) {
        if (NULL == (dump_sgc_destructor = xdl_sym(handler, SWAN_DUMP_LIBART_SGC_DESTRUCTOR, NULL))) goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", SWAN_DUMP_LIBART_SGC_DESTRUCTOR, (uintptr_t) dump_sgc_destructor);

        if (NULL == (dump_ssa_constructor = xdl_sym(handler, SWAN_DUMP_LIBART_SSA_CONSTRUCTOR, NULL))) goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", SWAN_DUMP_LIBART_SSA_CONSTRUCTOR, (uintptr_t) dump_ssa_constructor);

        if (NULL == (dump_ssa_destructor = xdl_sym(handler, SWAN_DUMP_LIBART_SSA_DESTRUCTOR, NULL))) goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", SWAN_DUMP_LIBART_SSA_DESTRUCTOR, (uintptr_t) dump_ssa_destructor);

        if (NULL == (dump_mutator_lock_ptr = xdl_sym(handler, SWAN_DUMP_LIBART_MUTATOR_LOCK_PTR, NULL))) goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", SWAN_DUMP_LIBART_MUTATOR_LOCK_PTR, (uintptr_t) dump_mutator_lock_ptr);

        if (NULL == (dump_exclusive_lock = xdl_sym(handler, SWAN_DUMP_LIBART_EXCLUSIVE_LOCK, NULL))) goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", SWAN_DUMP_LIBART_EXCLUSIVE_LOCK, (uintptr_t) dump_exclusive_lock);

        if (NULL == (dump_exclusive_unlock = xdl_sym(handler, SWAN_DUMP_LIBART_EXCLUSIVE_UNLOCK, NULL))) goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", SWAN_DUMP_LIBART_EXCLUSIVE_UNLOCK, (uintptr_t) dump_exclusive_unlock);

        if (NULL == (dump_sgc_constructor = xdl_sym(handler, SWAN_DUMP_LIBART_SGC_CONSTRUCTOR, NULL))) goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", SWAN_DUMP_LIBART_SGC_CONSTRUCTOR, (uintptr_t) dump_sgc_constructor);

       swan_libart_sgc_instance = malloc(64);
       swan_libart_ssa_instance = malloc(64);
    }

    swan_dump_symbols_status = 0;
  end:
    if (NULL != handler) xdl_close(handler);
    return swan_dump_symbols_status;
}

pid_t swan_dump_suspend(int wait) {
    if (0 != swan_dump_symbols_status) return -1;
    if (swan_common_api_level < __ANDROID_API_R__) {
        dump_libart_suspend();
    } else if (swan_common_api_level <= __ANDROID_API_S__) {
        void *self = __get_tls()[TLS_SLOT_ART_THREAD_SELF];
        dump_sgc_constructor(swan_libart_sgc_instance, self, kGcCauseHprof, kCollectorTypeHprof);
        dump_ssa_constructor(swan_libart_ssa_instance, "HprofDump", 1);
        // avoid deadlock with child process
        dump_exclusive_unlock(*dump_mutator_lock_ptr, self);
        dump_sgc_destructor(swan_libart_sgc_instance);
    }

    pid_t pid = fork();
    if (pid == 0) {
        // child process, set timeout for child process
        alarm(wait);
        prctl(PR_SET_NAME, "forked-dump-process");
    }
    return pid;
}

int swan_dump_resumed(pid_t pid) {
    if (0 != swan_dump_symbols_status) return -1;

    if (swan_common_api_level < __ANDROID_API_R__) {
        dump_libart_resumed();
    } else if (swan_common_api_level <= __ANDROID_API_S__) {
        void *self = __get_tls()[TLS_SLOT_ART_THREAD_SELF];
        dump_exclusive_lock(*dump_mutator_lock_ptr, self);
        dump_ssa_destructor(swan_libart_ssa_instance);
    }

    int status;
    for (;;) {
        if (waitpid(pid, &status, 0) != -1 || errno != EINTR) {
            if (!WIFEXITED(status)) {
                LOGE("Child process %d exited with status %d, terminated by signal %d",
                     pid, WEXITSTATUS(status), WTERMSIG(status));
                return SWAN_ERRNO_STATE;
            }
            return 0;
        }
        return SWAN_ERRNO_STATE;
    }

}
