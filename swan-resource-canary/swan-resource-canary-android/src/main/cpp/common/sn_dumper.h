//
// Created by YoungTr on 2022/4/14.
//

#ifndef SWAN_SN_DUMPER_H
#define SWAN_SN_DUMPER_H

#ifdef __cplusplus
extern "C" {
#endif

#ifndef __LP64__
#define SWAN_UTIL_LIBC        "/system/lib/libc.so"
#define SWAN_UTIL_LIBCPP      "/system/lib/libc++.so"
#define SWAN_UTIL_LIBART      "/system/lib/libart.so"
#define SWAN_UTIL_LIBC_APEX   "/apex/com.android.runtime/lib/bionic/libc.so"
#define SWAN_UTIL_LIBCPP_APEX "/apex/com.android.runtime/lib/libc++.so"
#define SWAN_UTIL_LIBART_APEX_29 "/apex/com.android.runtime/lib/libart.so"
#define SWAN_UTIL_LIBART_APEX_30 "/apex/com.android.art/lib/libart.so"
#else
#define SWAN_UTIL_LIBC        "/system/lib64/libc.so"
#define SWAN_UTIL_LIBCPP      "/system/lib64/libc++.so"
#define SWAN_UTIL_LIBART      "/system/lib64/libart.so"
#define SWAN_UTIL_LIBC_APEX   "/apex/com.android.runtime/lib64/bionic/libc.so"
#define SWAN_UTIL_LIBCPP_APEX "/apex/com.android.runtime/lib64/libc++.so"
#define SWAN_UTIL_LIBART_APEX_29 "/apex/com.android.runtime/lib64/libart.so"
#define SWAN_UTIL_LIBART_APEX_30 "/apex/com.android.art/lib64/libart.so"
#endif

#define SWAN_UTIL_LIBC_ABORT_MSG_PTR      "__abort_message_ptr"
#define SWAN_UTIL_LIBC_SET_ABORT_MSG      "android_set_abort_message"
#define SWAN_UTIL_LIBCPP_CERR             "_ZNSt3__14cerrE"
#define SWAN_UTIL_LIBART_RUNTIME_INSTANCE "_ZN3art7Runtime9instance_E"
#define SWAN_UTIL_LIBART_RUNTIME_DUMP     "_ZN3art7Runtime14DumpForSigQuitERNSt3__113basic_ostreamIcNS1_11char_traitsIcEEEE"
#define SWAN_UTIL_LIBART_THREAD_CURRENT   "_ZN3art6Thread14CurrentFromGdbEv"
#define SWAN_UTIL_LIBART_THREAD_DUMP      "_ZNK3art6Thread13DumpJavaStackERNSt3__113basic_ostreamIcNS1_11char_traitsIcEEEE"
#define SWAN_UTIL_LIBART_THREAD_DUMP2     "_ZNK3art6Thread13DumpJavaStackERNSt3__113basic_ostreamIcNS1_11char_traitsIcEEEEbb"
#define SWAN_UTIL_LIBART_DBG_SUSPEND      "_ZN3art3Dbg9SuspendVMEv"
#define SWAN_UTIL_LIBART_DBG_RESUME       "_ZN3art3Dbg8ResumeVMEv"

typedef void  (*sn_util_libart_dbg_suspend_t)(void);

typedef void  (*sn_util_libart_dbg_resume_t)(void);

int initialize();

void suspended();

void resumed();


#ifdef __cplusplus
}
#endif

#endif //SWAN_SN_DUMPER_H
