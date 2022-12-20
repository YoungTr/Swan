//
// Created by YoungTr on 2022/12/17.
//


#include <xhook.h>
#include <HookCommon.h>
#include <unistd.h>
#include "PthreadHook.h"
#include "ThreadTrace.h"
#include "ThreadStackShink.h"
#include "../common/Log.h"

#define LOG_TAG "Matrix.PthreadHook"
#define ORIGINAL_LIB "libc.so"

static volatile bool sThreadTraceEnabled = false;
static volatile bool sThreadStackShrinkEnabled = false;

DECLARE_HOOK_ORIG(int, pthread_create, pthread_t*, pthread_attr_t const*,
                  pthread_hook::pthread_routine_t, void*);

//typedef int (*fn_pthread_setname_np_t)(pthread_t, const char *);
//extern fn_pthread_setname_np_t orig_fn_pthread_setname_np;
//int h_pthread_setname_np(pthread_t, const char *);
DECLARE_HOOK_ORIG(int, pthread_setname_np, pthread_t, const char*);

DECLARE_HOOK_ORIG(int, pthread_detach, pthread_t);
DECLARE_HOOK_ORIG(int, pthread_join, pthread_t, void**);



DEFINE_HOOK_FUN(int, pthread_create, pthread_t *pthread, pthread_attr_t const *attr,
                pthread_hook::pthread_routine_t start_routine, void *args) {

    LOGD(LOG_TAG, "pthread_create");

    Dl_info callerInfo = {};
    bool callerInfoOk = true;
    if (0 == dladdr(__builtin_return_address(0), &callerInfo)) {
        LOGE(LOG_TAG, "%d >> Fail to get caller info.", ::getpid());
        callerInfoOk = false;
    }

    pthread_attr_t tmpAttr;
    if (LIKELY( attr == nullptr)) {
        int ret = pthread_attr_init(&tmpAttr);
        if (UNLIKELY(ret != 0)) {
            LOGE(LOG_TAG, "Fail to init new attr, ret: %d", ret);
        }
    } else {
        tmpAttr = *attr;
    }

    if (callerInfoOk && sThreadStackShrinkEnabled) {
        thread_stack_shink::OnPThreadCreate(&callerInfo, pthread, &tmpAttr, start_routine, args);
    }

    int ret;

    if (sThreadTraceEnabled) {
        auto *routine_wrapper = thread_trace::wrap_pthread_routine(start_routine, args);
        CALL_ORIGIN_FUNC_RET(int, tmpRet, pthread_create, pthread, &tmpAttr,
                             routine_wrapper->wrapped_func,
                             routine_wrapper);
        ret = tmpRet;
    } else {
        CALL_ORIGIN_FUNC_RET(int, tmpRet, pthread_create, pthread, &tmpAttr, start_routine, args);
        ret = tmpRet;
    }

    if (LIKELY(ret == 0) && sThreadTraceEnabled) {
        thread_trace::handle_pthread_create(*pthread);
    }

    if (LIKELY(attr == nullptr)) {
        pthread_attr_destroy(&tmpAttr);
    }


    return ret;

}

//fn_pthread_setname_np_t orig_fn_pthread_setname_np;
//int h_pthread_setname_np(pthread_t pthread, const char *name) {
//
//}
DEFINE_HOOK_FUN(int, pthread_setname_np, pthread_t pthread, const char* name) {
    LOGD(LOG_TAG, "pthread_setname_np");
    CALL_ORIGIN_FUNC_RET(int, ret, pthread_setname_np, pthread, name);
    if (LIKELY(ret == 0) && sThreadTraceEnabled) {
        thread_trace::handle_pthread_setname_np(pthread, name);
    }
    return ret;
}

DEFINE_HOOK_FUN(int, pthread_detach, pthread_t pthread) {
    CALL_ORIGIN_FUNC_RET(int, ret, pthread_detach, pthread);
    LOGD(LOG_TAG, "pthread_detach : %d", ret);
    if (LIKELY(ret == 0) && sThreadTraceEnabled) {
        thread_trace::handle_pthread_release(pthread);
    }
    return ret;
}


DEFINE_HOOK_FUN(int, pthread_join, pthread_t pthread, void** return_value_ptr) {
    CALL_ORIGIN_FUNC_RET(int, ret, pthread_join, pthread, return_value_ptr);
    LOGD(LOG_TAG, "pthread_join : %d", ret);
    if (LIKELY(ret == 0) && sThreadTraceEnabled) {
        thread_trace::handle_pthread_release(pthread);
    }
    return ret;
}

namespace pthread_hook {

    void SetThreadTraceEnabled(bool enabled) {
        LOGD(LOG_TAG, "[*] Calling SetThreadTraceEnabled, enabled: %d", enabled);
        sThreadTraceEnabled = enabled;
    }

    void SetThreadStackShrinkEnabled(bool enabled) {
        LOGD(LOG_TAG, "[*] Calling SetThreadStackShrinkEnabled, enabled: %d", enabled);
        sThreadStackShrinkEnabled = enabled;
    }

    void SetThreadStackShrinkIgnoredCreatorSoPatterns(const char** patterns, size_t pattern_count) {
        LOGD(LOG_TAG, "[*] Calling SetThreadStackShrinkIgnoredCreatorSoPatterns, patterns: %p, count: %d",
             patterns, pattern_count);
        thread_stack_shink::SetIgnoredCreatorSoPatterns(patterns, pattern_count);
    }

    void InstallHooks(bool enable_debug) {
        LOGI(LOG_TAG,
             "[+] Calling InstallHooks, sThreadTraceEnabled: %d, sThreadStackShinkEnabled: %d",
             sThreadTraceEnabled, sThreadStackShrinkEnabled);

        if (!sThreadTraceEnabled && !sThreadStackShrinkEnabled) {
            LOGD(LOG_TAG, "[*] InstallHooks was ignored.");
            return;
        }
        FETCH_ORIGIN_FUNC(pthread_create)
        FETCH_ORIGIN_FUNC(pthread_setname_np)
        FETCH_ORIGIN_FUNC(pthread_detach)
        FETCH_ORIGIN_FUNC(pthread_join)

        if (sThreadTraceEnabled) {
            thread_trace::thread_trace_init();
        }

        // do hook
        {
            xhook_register(".*/.*\\.so$",
                           "pthread_create",
                           (void *) HANDLER_FUNC_NAME(pthread_create),
                           nullptr);
            xhook_register(".*/.*\\.so$",
                           "pthread_setname_np",
                           (void *) HANDLER_FUNC_NAME(pthread_setname_np),
                           nullptr);
            xhook_register(".*/.*\\.so$",
                           "pthread_detach",
                           (void *) HANDLER_FUNC_NAME(pthread_detach),
                           nullptr);
            xhook_register(".*/.*\\.so$",
                           "pthread_join",
                           (void *) HANDLER_FUNC_NAME(pthread_join),
                           nullptr);

            xhook_enable_debug(enable_debug ? 1 : 0);
            xhook_enable_sigsegv_protection(0);
            xhook_refresh(0);
        }


    }
}