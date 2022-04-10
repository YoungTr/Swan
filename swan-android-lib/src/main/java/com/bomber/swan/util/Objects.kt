package com.bomber.swan.util

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * @author youngtr
 * @data 2022/4/10
 */

internal inline fun <reified T : Any> noOpDelegate(): T {
    val javaClass = T::class.java
    return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(javaClass), NO_OP_HANDLER) as T
}

private val NO_OP_HANDLER = InvocationHandler { _, _, _ ->
    // no operate
}