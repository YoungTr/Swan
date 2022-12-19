//
// Created by YoungTr on 2022/12/17.
//

#include <regex.h>
#include <mutex>
#include <map>
#include <set>
#include <Log.h>
#include "Backtrace.h"
#include "ThreadTrace.h"
#include <ReentrantPrevention.h>


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

static void on_pthread_exit(void *specific) {

}

void thread_trace::enable_quicken_unwind(const bool enable) {

}

void thread_trace::enable_trace_pthread_release(const bool enable) {

}


thread_trace::routine_wrapper_t *
thread_trace::wrap_pthread_routine(pthread_hook::pthread_routine_t start_routine, void *args) {
    return nullptr;
}


void thread_trace::handle_pthread_create(const pthread_t pthread) {}

void thread_trace::handle_pthread_setname_np(pthread_t pthread, const char *name) {}

void thread_trace::handle_pthread_release(pthread_t pthread) {}










