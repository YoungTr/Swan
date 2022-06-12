//
// Created by YoungTr on 2022/6/10.
//

#include <signal.h>
#include <string.h>
#include <sys/eventfd.h>
#include  <pthread.h>
#include <errno.h>
#include <unistd.h>
#include "signal_handler.h"
#include "trace_common.h"
#include "swan_trace.h"
#include "log.h"

static sigset_t signal_trace_oldset;
static struct sigaction signal_trace_oldact;

static int trace_notifier = -1;

static void trace_handler(int sig, siginfo_t *si, void *uc) {
    uint64_t data;

    (void) sig;
    (void) si;
    (void) uc;

    if (trace_notifier >= 0) {
        data = 1;
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
    LOGD("signal register success");
    return 0;

}

static void *trace_dump(void *arg) {
    JNIEnv *env = NULL;
    uint64_t data;
    uint64_t trace_time;
    int fd;
    struct timeval tv;
    char pathname[1024];
    jstring j_pathname;

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

        anrDumpCallback(env);
        JNI_IGNORE_PENDING_EXCEPTION();


    }

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





