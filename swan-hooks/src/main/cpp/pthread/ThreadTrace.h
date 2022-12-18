//
// Created by YoungTr on 2022/12/17.
//

#ifndef SWAN_THREADTRACE_H
#define SWAN_THREADTRACE_H

#include <pthread.h>
#include "PthreadHook.h"


namespace thread_trace {
    void thread_trace_init();

    void add_hook_thread_name(const char *regex_str);

    void pthread_dump_json(const char *path);

    void enable_quicken_unwind(const bool enable);

    void enable_trace_pthread_release(const bool enable);

    typedef struct {
        pthread_hook::pthread_routine_t wrapped_func;
        pthread_hook::pthread_routine_t origin_func;
        void *origin_args;
    } routine_wrapper_t;

    routine_wrapper_t* wrap_pthread_routine(pthread_hook::pthread_routine_t start_routine, void* args);

    void handle_pthread_create(const pthread_t pthread);

    void handle_pthread_setname_np(pthread_t pthread, const char* name);

    void handle_pthread_release(pthread_t pthread);
}

#endif //SWAN_THREADTRACE_H
