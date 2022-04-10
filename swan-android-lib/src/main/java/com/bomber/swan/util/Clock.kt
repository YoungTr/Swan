package com.bomber.swan.util

/**
 * @author youngtr
 * @data 2022/4/10
 *
 * 函数式（SAM）接口
 * https://www.kotlincn.net/docs/reference/fun-interfaces.html
 */
fun interface Clock {

    fun uptimeMillis(): Long

    companion object {

        inline operator fun invoke(crossinline block: () -> Long): Clock =
            Clock { block() }
    }
}