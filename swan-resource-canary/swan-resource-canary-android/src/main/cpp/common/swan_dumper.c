//
// Created by YoungTr on 2022/4/14.
//

#include <inttypes.h>
#include <unistd.h>
#include <sys/prctl.h>
#include <sys/wait.h>
#include "log.h"
#include "xdl.h"
#include "swan_errno.h"
#include "swan_dumper.h"
#include "swan_common.h"

static int swan_dump_symbols_loaded = 0;
static int swan_dump_symbols_status = SWAN_ERRNO_NOTFND;

static swan_libart_suspend dump_libart_suspend = NULL;
static swan_libart_resumed dump_libart_resumed = NULL;


int swan_dump_init() {

    LOGD("app level: %d", swan_common_api_level);
    // only init once
    if (swan_dump_symbols_loaded) return swan_dump_symbols_status;
    swan_dump_symbols_loaded = 1;

    void *handler;

    if (NULL == (handler = xdl_open(SWAN_DUMP_LIB_ART, XDL_DEFAULT))) goto end;

    LOGD(">>> xdl_open(%s) : handle %" PRIxPTR, "libart.so", (uintptr_t) handler);

    if (swan_common_api_level < __ANDROID_API_R__) { // < android 11
        if (NULL ==
            (dump_libart_resumed = xdl_sym(handler, SWAN_UTIL_LIBART_DBG_RESUME, NULL)))
            goto end;
        if (NULL ==
            (dump_libart_suspend = xdl_sym(handler, SWAN_UTIL_LIBART_DBG_SUSPEND, NULL)))
            goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym",
             SWAN_UTIL_LIBART_DBG_RESUME, (uintptr_t) dump_libart_resumed);

        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym",
             SWAN_UTIL_LIBART_DBG_SUSPEND, (uintptr_t) dump_libart_suspend);
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
