@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "NOTHING_TO_INLINE")
@file:JvmName("swan-trace-canary_Friendly")

package com.bomber.swan.trace.friendly

/**
 * @author youngtr
 * @data 2022/4/10
 */

internal inline val mainHandler
    get() = com.bomber.swan.util.mainHandler

internal inline fun checkMainThread() = com.bomber.swan.util.checkMainThread()
