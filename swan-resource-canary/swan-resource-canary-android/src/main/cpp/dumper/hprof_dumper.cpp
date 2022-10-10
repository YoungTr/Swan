//
// Created by YoungTr on 2022/9/22.
//

#include <xdl.h>
#include <cinttypes>
#include <unistd.h>
#include <linux/prctl.h>
#include <sys/prctl.h>
#include <sys/wait.h>
#include <cerrno>
#include "tls.h"
#include "hprof_dumper.h"
#include "log.h"

HprofDumper &HprofDumper::getInstance() {
    static HprofDumper hprofDumper;
    return hprofDumper;
}


HprofDumper::HprofDumper() : init_done(false), android_api(android_get_device_api_level()) {}

void HprofDumper::initialize() {
    if (init_done || android_api < __ANDROID_API_L__) return;

    void *handler;

    if (nullptr == (handler = xdl_open("libart.so", XDL_DEFAULT))) goto end;
    if (android_api < __ANDROID_API_R__) {  // < Android 11
        if (nullptr == (suspend_vm_fnc = (void (*)()) xdl_sym(handler, "_ZN3art3Dbg9SuspendVMEv", nullptr))) goto end;
        if (nullptr == (resume_vm_fnc = (void (*)()) xdl_sym(handler, "_ZN3art3Dbg8ResumeVMEv", nullptr))) goto end;
        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", "_ZN3art3Dbg9SuspendVMEv", (uintptr_t) suspend_vm_fnc);

        LOGD(">>> %s(%s) : addr %" PRIxPTR " ", "xdl_sym", "_ZN3art3Dbg8ResumeVMEv", (uintptr_t) resume_vm_fnc);
    } else if (android_api <= __ANDROID_API_S__) {
        ssa_instance = std::make_unique<char[]>(64);
        sgc_instance = std::make_unique<char[]>(64);

        if (nullptr == (ssa_constructor_fnc = (void (*)(void *,const char *,bool )) xdl_sym(handler,"_ZN3art16ScopedSuspendAllC1EPKcb",nullptr)))
            goto end;
        if (nullptr == (ssa_destructor_fnc = (void (*)(void *)) xdl_sym(handler, "_ZN3art16ScopedSuspendAllD1Ev", nullptr)))
            goto end;
        if (nullptr == (sgc_constructor_fnc = (void (*)(void *, void *, GcCause, CollectorType)) xdl_sym(handler,"_ZN3art2gc23ScopedGCCriticalSectionC1EPNS_6ThreadENS0_7GcCauseENS0_13CollectorTypeE",nullptr)))
            goto end;
        if (nullptr == (sgc_destructor_fnc = (void (*)(void *)) xdl_sym(handler, "_ZN3art2gc23ScopedGCCriticalSectionD1Ev", nullptr)))
            goto end;
        if (nullptr == (mutator_lock_ptr = (void **) xdl_sym(handler, "_ZN3art5Locks13mutator_lock_E", nullptr)))
            goto end;
        if (nullptr == (exclusive_lock_fnc = (void (*)(void *, void *)) xdl_sym(handler, "_ZN3art17ReaderWriterMutex13ExclusiveLockEPNS_6ThreadE", nullptr)))
            goto end;
        if (nullptr == (exclusive_unlock_fnc = (void (*)(void *, void *)) xdl_sym(handler, "_ZN3art17ReaderWriterMutex15ExclusiveUnlockEPNS_6ThreadE", nullptr)))
            goto end;

    }
    init_done = true;


  end:
    if (nullptr != handler) xdl_close(handler);
}

pid_t HprofDumper::suspendAndFork() {
    if (!init_done) return -1;
    if (android_api < __ANDROID_API_R__) {
        LOGD("suspend");
        suspend_vm_fnc();
    } else if (android_api <= __ANDROID_API_S__) {
        void *self = __get_tls()[TLS_SLOT_ART_THREAD_SELF];
        sgc_constructor_fnc((void *) sgc_instance.get(), self, kGcCauseHprof, kCollectorTypeHprof);
        ssa_constructor_fnc((void *) ssa_instance.get(), "HprofDump", true);
        // avoid deadlock with child process
        exclusive_unlock_fnc(*mutator_lock_ptr, self);
        sgc_destructor_fnc((void *) sgc_instance.get());

    }
    pid_t pid = fork();
    LOGD("fork");
    if (pid == 0) {
        alarm(60);
        prctl(PR_SET_NAME, "fork-dump-process");
    }
    return pid;
}

bool HprofDumper::resumeAndWait(pid_t pid) {
    if (!init_done) return false;
    if (android_api < __ANDROID_API_R__) {
        LOGD("resume");
        resume_vm_fnc();
    } else if (android_api <= __ANDROID_API_S__) {
        void *self = __get_tls()[TLS_SLOT_ART_THREAD_SELF];
        exclusive_lock_fnc(*mutator_lock_ptr, self);
        ssa_destructor_fnc((void *) ssa_instance.get());
    }
    int status;
    for (;;) {
        if (waitpid(pid, &status, 0) != -1 || errno != EINTR) {
            if (!WIFEXITED(status)) {
                LOGD("Child process %d exited with status %d, terminated by signal %d",
                      pid, WEXITSTATUS(status), WTERMSIG(status));
                return false;
            }
            return true;
        }
        return false;
    }
}

