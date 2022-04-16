package com.bomber.swan.util

import android.os.SystemClock

/**
 * @author youngtr
 * @data 2022/4/16
 */

internal inline fun measureDurationMillis(block: () -> Unit): Long {
    val start = SystemClock.uptimeMillis()
    block()
    return SystemClock.uptimeMillis() - start
}