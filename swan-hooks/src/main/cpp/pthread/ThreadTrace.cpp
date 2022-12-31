//
// Created by YoungTr on 2022/12/17.
//

#include <regex.h>
#include <mutex>
#include <map>
#include <set>
#include "../common/Log.h"
#include "Backtrace.h"
#include "ThreadTrace.h"
#include <ReentrantPrevention.h>
#include "HookCommon.h"


#define ORIGINAL_LIB "libc.so"
#define TAG "Matrix.PthreadHook"

#define THREAD_NAME_LEN 16
#define PTHREAD_BACKTRACE_MAX_FRAMES MAX_FRAME_SHORT
#define PTHREAD_BACKTRACE_MAX_FRAMES_LONG MAX_FRAME_LONG_LONG
#define PTHREAD_BACKTRACE_FRAME_ELEMENTS_MAX_SIZE MAX_FRAME_NORMAL

typedef void *(*pthread_routine_t)(void *);

static volatile bool m_quicken_unwind = false;
static volatile size_t m_pthread_backtrace_max_frames =
        m_quicken_unwind ? PTHREAD_BACKTRACE_MAX_FRAMES_LONG
                         : PTHREAD_BACKTRACE_MAX_FRAMES;

static volatile bool m_trace_pthread_release = false;

struct pthread_meta_t {
    pid_t tid;
    char *thread_name;

    wechat_backtrace::BacktraceMode unwind_mode;
    uint64_t hash;
    wechat_backtrace::Backtrace native_backtrace;
    std::atomic<char *> java_stacktrace;

    bool exited;

    pthread_meta_t() : tid(0),
                       thread_name(nullptr),
                       unwind_mode(wechat_backtrace::FramePointer),
                       hash(0),
                       native_backtrace(BACKTRACE_INITIALIZER(m_pthread_backtrace_max_frames)),
                       java_stacktrace(nullptr),
                       exited(false) {}

    ~pthread_meta_t() = default;

    pthread_meta_t(const pthread_meta_t &src) {
        tid = src.tid;
        thread_name = src.thread_name;
        unwind_mode = src.unwind_mode;
        hash = src.hash;
        native_backtrace = src.native_backtrace;
        java_stacktrace.store(src.java_stacktrace.load(std::memory_order_acquire),
                              std::memory_order_release);
        exited = src.exited;
    }

};

struct regex_wrapper {
    const char *regex_str;
    regex_t regex;

    regex_wrapper(const char *regexStr, const regex_t &regex) : regex_str(regexStr), regex(regex) {}

    friend bool operator<(const regex_wrapper &left, const regex_wrapper &right) {
        return static_cast<bool>(strcmp(left.regex_str, right.regex_str));
    }
};

static std::mutex m_pthread_meta_mutex;
static std::timed_mutex m_java_stacktrace_mutex;
typedef std::lock_guard<std::mutex> pthread_meta_lock;

static std::map<pthread_t, pthread_meta_t> m_pthread_metas;
static std::set<pthread_t> m_filtered_pthreads;

static std::set<regex_wrapper> m_hook_thread_name_regex;
static pthread_key_t m_destructor_key;

static std::mutex m_subroutine_mutex;
static std::condition_variable m_subroutine_cv;

static std::set<pthread_t> m_pthread_routine_flags;

static void erase_meta(std::map<pthread_t, pthread_meta_t> &metas, pthread_t &pthread, pthread_meta_t &meta) {
    free(meta.thread_name);

    char *java_stacktrace = meta.java_stacktrace.load(std::memory_order_acquire);
    if (java_stacktrace) {
        free(java_stacktrace);
    }

    metas.erase(pthread);
}

static void on_pthread_exit(void *specific);


void thread_trace::thread_trace_init() {
    LOGD(TAG, "thread_trace_init");

    if (!m_destructor_key) {
        pthread_key_create(&m_destructor_key, on_pthread_exit);
    }

    rp_init();

}

void thread_trace::add_hook_thread_name(const char *regex_str) {
    regex_t regex;
    if (0 != regcomp(&regex, regex_str, REG_NOSUB)) {
        LOGE("PthreadHook", "regex compiled error: %s", regex_str);
        return;
    }

    size_t len = strlen(regex_str) + 1;
    char *p_regex_str = static_cast<char *>(malloc(len));
    strncpy(p_regex_str, regex_str, len);
    regex_wrapper w_regex(p_regex_str, regex);
    m_hook_thread_name_regex.insert(w_regex);
    LOGD(TAG, "parent name regex: %s -> %s, len = %zu", regex_str, p_regex_str, len);
}


void thread_trace::pthread_dump_json(const char *path) {

}

static void notify_routine(const pthread_t pthread) {
    std::lock_guard<std::mutex> routine_lock(m_subroutine_mutex);

    m_pthread_routine_flags.emplace(pthread);
    LOGD(TAG, "notify waiting count : %zu", m_pthread_routine_flags.size());

    m_subroutine_cv.notify_all();
}

static void on_pthread_exit(void *specific) {
    LOGD(TAG, "on_pthread_exit");
    if (specific) {
        free(specific);
    }

    pthread_t exiting_thread = pthread_self();
    if (!m_pthread_metas.count(exiting_thread)) {
        LOGD(TAG, "on_pthread_exit: thread not found");
        return;
    }

    pthread_meta_t &meta = m_pthread_metas.at(exiting_thread);
    m_filtered_pthreads.erase(exiting_thread);

    LOGD(TAG, "gonna remove thread {%ld, %s, %d}", exiting_thread, meta.thread_name, meta.tid);
    pthread_attr_t attr;
    pthread_getattr_np(exiting_thread, &attr);
    int state = PTHREAD_CREATE_JOINABLE;
    pthread_attr_getdetachstate(&attr, &state);

    if (m_trace_pthread_release && state != PTHREAD_CREATE_DETACHED) {
        LOGD(TAG, "just mark exited");
        meta.exited = true;
    } else {
        LOGD(TAG, "real removed");
        erase_meta(m_pthread_metas, exiting_thread, meta);
    }


}

void thread_trace::enable_quicken_unwind(const bool enable) {

}

void thread_trace::enable_trace_pthread_release(const bool enable) {

}

static inline void before_routine_start() {
    LOGI(TAG, "before_routine_start");
    std::unique_lock<std::mutex> routine_lock(m_subroutine_mutex);
    LOGI(TAG, "before_routine_start lock");


    pthread_t self_thread = pthread_self();

    m_subroutine_cv.wait(routine_lock, [&self_thread] {
        return m_pthread_routine_flags.count(self_thread);
    });

    LOGI(TAG, "before_routine_start: create ready, just continue, waiting count : %zu",
         m_pthread_routine_flags.size());

    m_pthread_routine_flags.erase(self_thread);
}

static void *pthread_routine_wrapper(void *arg) {
    LOGI(TAG, "pthread_routine_wrapper");

    auto *specific = (char *) malloc(sizeof(char));
    *specific = 'P';

    pthread_setspecific(m_destructor_key, specific);

    before_routine_start();

    auto *args_wrapper = (thread_trace::routine_wrapper_t *) arg;
    void *ret          = args_wrapper->origin_func(args_wrapper->origin_args);
    free(args_wrapper);
    return ret;
}


thread_trace::routine_wrapper_t *
thread_trace::wrap_pthread_routine(pthread_hook::pthread_routine_t start_routine, void *args) {
    auto routine_wrapper = (thread_trace::routine_wrapper_t *) malloc(
            sizeof(thread_trace::routine_wrapper_t));
    routine_wrapper->wrapped_func = pthread_routine_wrapper;
    routine_wrapper->origin_func  = start_routine;
    routine_wrapper->origin_args  = args;
    return routine_wrapper;
}


void thread_trace::handle_pthread_create(const pthread_t pthread) {
    LOGD(TAG, "handle_pthread_create");
    const char *arch =
#ifdef __aarch64__
            "aarch64";
#elif defined __arm__
    "arm";
#endif
    LOGD(TAG, "+++++++ on_pthread_create, %s", arch);

    pid_t tid = pthread_gettid_np(pthread);

    if (!rp_acquire()) {
        LOGD(TAG, "reentrant!!!");
        notify_routine(pthread);
        return;
    }

    if (!m_quicken_unwind) {
        const size_t BUF_SIZE         = 1024;
        char         *java_stacktrace = static_cast<char *>(malloc(BUF_SIZE));
        strncpy(java_stacktrace, "(init stacktrace)", BUF_SIZE);
        if (m_java_stacktrace_mutex.try_lock_for(std::chrono::milliseconds(100))) {
            if (java_stacktrace) {
                get_java_stacktrace(java_stacktrace, BUF_SIZE);
            }
            m_java_stacktrace_mutex.unlock();
        } else {
            LOGE(TAG, "maybe reentrant!");
        }

        LOGD(TAG, "parent_tid: %d -> tid: %d", pthread_gettid_np(pthread_self()), tid);
//        bool recorded = on_pthread_create_locked(pthread, java_stacktrace, false, tid);
        bool recorded = true;

        if (!recorded && java_stacktrace) {
            free(java_stacktrace);
        }
    } else {
        LOGD(TAG, "parent_tid: %d -> tid: %d", pthread_gettid_np(pthread_self()), tid);
//        on_pthread_create_locked(pthread, nullptr, true, tid);
    }

//
    rp_release();
    notify_routine(pthread);
    LOGD(TAG, "------ on_pthread_create end");
}

void thread_trace::handle_pthread_setname_np(pthread_t pthread, const char *name) {}

void thread_trace::handle_pthread_release(pthread_t pthread) {}










