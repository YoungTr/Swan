package com.bomber.swan.trace.listeners

import androidx.annotation.CallSuper

/**
 * @author youngtr
 * @data 2022/5/21
 */
abstract class LooperObserver(private var isDispatchBegin: Boolean = false) {

    @CallSuper
    open fun dispatchBegin(beginNs: Long, cpuBeginNs: Long, token: Long) {
        isDispatchBegin = true
    }

    open fun doFrame(
        focusedActivity: String?,
        startNs: Long,
        endNs: Long,
        isVsyncFrame: Boolean,
        intendedFrameTimeNs: Long,
        inputCostNs: Long,
        animationCostNs: Long,
        traversalCostNs: Long
    ) {
    }

    @CallSuper
    open fun dispatchEnd(
        beginNs: Long,
        cpuBeginMs: Long,
        endNs: Long,
        cpuEndMs: Long,
        token: Long,
        isVsyncFrame: Boolean
    ) {
        isDispatchBegin = false
    }

    fun isDispatchBegin(): Boolean {
        return isDispatchBegin
    }

    fun isForeground(): Boolean {
        // TODO:
        return true
    }

}