@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "NOTHING_TO_INLINE")
@file:JvmName("swan-object-watcher-android_Friendly")

package com.bomber.swan.resource.friendly

/**
 * @author youngtr
 * @data 2022/4/10
 */

internal inline val mainHandler
    get() = com.bomber.swan.util.mainHandler

internal inline fun checkMainThread() = com.bomber.swan.util.checkMainThread()

internal inline fun <reified T : Any> noOpDelegate(): T = com.bomber.swan.util.noOpDelegate()