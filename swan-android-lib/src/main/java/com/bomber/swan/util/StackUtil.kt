package com.bomber.swan.util

import android.os.Looper
import kotlin.math.min

@JvmOverloads
fun getStack(
    trace: Array<StackTraceElement> = Throwable().stackTrace,
    preFix: String = "",
    limit: Int = -1,
    start: Int = 3
): String {
    if (trace.size < 3) return ""
    val lenLimit = if (limit < 0) Int.MAX_VALUE else limit
    val loop = min(trace.size - start, lenLimit)
    val sb = StringBuilder(" \n")
    for (index in start until loop) {
        sb.append(preFix)
            .append("at ")
            .append(trace[index].className)
            .append(":")
            .append(trace[index].methodName)
            .append("(${trace[index].lineNumber})")
            .append("\n")
    }
    return sb.toString()
}

fun getWholeStack(trace: Array<StackTraceElement>): String {
    val sb = StringBuilder()
    for (element in trace) {
        sb.append(element.toString()).append("\n")
    }
    return sb.toString()
}

fun printException(e: Exception): String {
    val stackTrace = e.stackTrace ?: return ""

    val t = java.lang.StringBuilder(e.toString())
    for (i in 2 until stackTrace.size) {
        t.append('[')
        t.append(stackTrace[i].className)
        t.append(':')
        t.append(stackTrace[i].methodName)
        t.append("(" + stackTrace[i].lineNumber + ")]")
        t.append("\n")
    }
    return t.toString()
}

fun getMainThreadJavaStackTrace(): String {
    val stackTrace = java.lang.StringBuilder()
    for (stackTraceElement in Looper.getMainLooper().thread.stackTrace) {
        stackTrace.append(stackTraceElement.toString()).append("\n")
    }
    return stackTrace.toString()
}
