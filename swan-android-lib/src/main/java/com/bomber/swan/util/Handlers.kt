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

object Global {
    val globalHandler: Handler
        get() {
            val handlerThread = newHandlerThread("global")
            handlerThread.start()
            return Handler(handlerThread.looper)
        }
}
