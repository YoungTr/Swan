package com.bomber.swan.trace.trace

import androidx.annotation.CallSuper
import com.bomber.swan.trace.listeners.LooperObserver
import com.bomber.swan.util.SwanLog

/**
 * @author youngtr
 * @data 2022/5/21
 */
abstract class Tracer : LooperObserver(), ITrace {
    override var isAlive: Boolean = false

    @CallSuper
    open fun onAlive() {
        SwanLog.i(TAG, "[onAlive] %s", this.javaClass.name)
    }

    @CallSuper
    open fun onDead() {
        SwanLog.i(TAG, "[onDead] %s", this.javaClass.name)

    }

    @Synchronized
    final override fun onStartTrace() {
        if (!isAlive) {
            this.isAlive = true
            onAlive()
        }
    }

    @Synchronized
    final override fun onCloseTrace() {
        if (isAlive) {
            this.isAlive = false
            onDead()
        }
    }

    override fun onForeground(isForeground: Boolean) {

    }


    companion object {
        private const val TAG = "Swan.Tracer"
    }

}