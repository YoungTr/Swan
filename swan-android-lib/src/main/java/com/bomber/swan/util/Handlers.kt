package com.bomber.swan.util

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

internal val mainHandler by lazy { Handler(Looper.getMainLooper()) }

internal val isMainThread: Boolean get() = Looper.getMainLooper().thread === Thread.currentThread()

internal fun checkMainThread() {
    check(isMainThread) {
        "Should be called from the main thread, not ${Thread.currentThread()}"
    }
}

internal fun checkNotMainThread() {
    check(!isMainThread) {
        "Should not be called from the main thread"
    }
}

fun newHandlerThread(name: String, priority: Int = Thread.NORM_PRIORITY): HandlerThread {
    val handlerThread = HandlerThread(name, priority)
    handlerThread.start()
    return handlerThread
}

/**
 * globalHandler 只允许执行一些轻量的工作
 * heavy work 使用 [newHandlerThread] 创建新的 thread
 */
val globalHandler: Handler by lazy {
    val handlerThread = newHandlerThread("global")
    Handler(handlerThread.looper)
}
