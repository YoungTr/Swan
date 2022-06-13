//
// Created by YoungTr on 2022/6/10.
//

#include <signal.h>
#include <string.h>
#include <sys/eventfd.h>
#include <sys/syscall.h>
#include  <pthread.h>
#include <errno.h>
#include <unistd.h>
#include <dirent.h>
#include <stdio.h>
#include <inttypes.h>
#include "signal_handler.h"
#include "trace_common.h"
#include "swan_trace.h"
#include "log.h"

#define SIGQUIT_FROM_INTERNAL  1
#define SIGQUIT_FROM_EXTERNAL  2

#define TRACE_SIGNAL_CATCHER_TID_UNLOAD (-2)
#define TRACE_SIGNAL_CATCHER_TID_UNKNOWN (-1)

#define TRACE_SIGNAL_CATCHER_THREAD_NAME   "Signal Catcher"
#define TRACE_SIGNAL_CATCHER_THREAD_SIGBLK 0x1000


static sigset_t signal_trace_oldset;
static struct sigaction signal_trace_oldact;

static int trace_notifier = -1;

static pid_t trace_signal_catcher_tid = TRACE_SIGNAL_CATCHER_TID_UNLOAD;

static void trace_load_signal_catcher_tid() {
    char buf[256];
    DIR *dir;
    struct dirent *ent;
    FILE *f;
    pid_t tid;
    uint64_t sigblk;

    trace_signal_catcher_tid = TRACE_SIGNAL_CATCHER_TID_UNKNOWN;
    // 设将可变参数(...)按照 format 格式化成字符串，并将字符串复制到 buf 中
    snprintf(buf, sizeof(buf), "/proc/%d/task", common_process_id);
    if (NULL == (dir = opendir(buf))) return;
    while (NULL != (ent = readdir(dir))) {
        // get and check thread id
        if (0 != util_atoi(ent->d_name, &tid)) continue;
        if (tid <= 0) continue;

        // check thread name
        snprintf(buf, sizeof(buf), "/proc/%d/comm", tid);
        util_get_thread_name(tid, buf, sizeof(buf));

        if (0 != strcmp(buf, TRACE_SIGNAL_CATCHER_THREAD_NAME)) continue;

        // check signal block masks
        sigblk = 0;
        snprintf(buf, sizeof(buf), "/proc/%d/status", tid);
        if (NULL == (f = fopen(buf, "r"))) break;
        while (fgets(buf, sizeof(buf), f)) {
            if (1 == sscanf(buf, "SigBlk: %"SCNx64, &sigblk)) break;
        }
        fclose(f);
        if (TRACE_SIGNAL_CATCHER_THREAD_SIGBLK != sigblk) continue;

        // found it
        trace_signal_catcher_tid = tid;
        break;
    }

    closedir(dir);

}


static void trace_handler(int sig, siginfo_t *si, void *uc) {
    uint64_t data;

    (void) sig;
    (void) uc;

    if (trace_notifier >= 0) {
        int fromPid1 = si->_si_pad[3];
        int fromPid2 = si->_si_pad[4];
        data = ((fromPid1 == common_process_id || fromPid2 == common_process_id) ? SIGQUIT_FROM_INTERNAL : SIGQUIT_FROM_EXTERNAL);
        TEMP_FAILURE_RETRY(write(trace_notifier, &data, sizeof(data)));
    }
}


static int signal_register(void (*handler)(int, siginfo_t *, void *)) {
    int r;
    sigset_t set;
    struct sigaction act;

    //un-block the SIGQUIT mask for current thread
    sigemptyset(&set);
    sigaddset(&set, SIGQUIT);
    if (0 != (r = pthread_sigmask(SIG_UNBLOCK, &set, &signal_trace_oldset))) return r;

    // register new signal handler for SIGQUIT
    memset(&act, 0, sizeof(act));
    sigfillset(&act.sa_mask);
    act.sa_sigaction = handler;
    act.sa_flags = SA_RESTART | SA_SIGINFO;
    if (0 != sigaction(SIGQUIT, &act, &signal_trace_oldact)) {
        pthread_sigmask(SIG_SETMASK, &signal_trace_oldset, NULL);
        return ERRNO_SYS;
    }
    return 0;

}

static void trace_send_sigquit() {
    if (TRACE_SIGNAL_CATCHER_TID_UNLOAD == trace_signal_catcher_tid) {
        trace_load_signal_catcher_tid();
    }

    if (trace_signal_catcher_tid >= 0) {
        syscall(SYS_tgkill, common_process_id, trace_signal_catcher_tid, SIGQUIT);
    }
}

static void *trace_dump(void *arg) {
    JNIEnv *env = NULL;
    uint64_t data;

    (void) arg;

    pthread_detach(pthread_self());

    JavaVMAttachArgs attach_args = {
            .version = SA_JNI_VERSION,
            .name    = "trace_dp",
            .group   = NULL
    };

    if (JNI_OK != (*common_vm)->AttachCurrentThread(common_vm, &env, &attach_args)) goto exit;

    while (1) {
        // block here, waiting for sigquit
        TEMP_FAILURE_RETRY(read(trace_notifier, &data, sizeof(data)));
        if (SIGQUIT_FROM_EXTERNAL == data) {
            anrDumpCallback(env);
        }
        trace_send_sigquit();
    }

    // 注意释放资源
    (*common_vm)->DetachCurrentThread(common_vm);

    exit:
    close(trace_notifier);
    trace_notifier = -1;
    return NULL;

}

int sa_signal_init(JNIEnv *env) {

    int r;
    pthread_t thd;

    // capture SIGQUIT only for ART
    if (common_api_level < 21) return 0;

    // create event fd
    if (0 > (trace_notifier = eventfd(0, EFD_CLOEXEC))) return ERRNO_SYS;

    // register signal handler
    if (0 != (r = signal_register(trace_handler))) return r;

    // create thread for dump trace
    if (0 != (r = pthread_create(&thd, NULL, trace_dump, NULL))) goto err2;

    return 0;

    err2:
    close(trace_notifier);
    trace_notifier = -1;
    return r;
}





