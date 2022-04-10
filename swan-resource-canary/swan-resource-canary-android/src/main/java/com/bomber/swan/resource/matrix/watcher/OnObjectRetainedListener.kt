package com.bomber.swan.resource.matrix.watcher

/**
 * @author youngtr
 * @data 2022/4/10
 */
fun interface OnObjectRetainedListener {

    fun onObjectRetained()

    companion object {
        inline operator fun invoke(crossinline block: () -> Unit): OnObjectRetainedListener =
            OnObjectRetainedListener { block() }
    }
}