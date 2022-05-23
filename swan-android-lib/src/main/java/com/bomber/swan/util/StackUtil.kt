package com.bomber.swan.util

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
