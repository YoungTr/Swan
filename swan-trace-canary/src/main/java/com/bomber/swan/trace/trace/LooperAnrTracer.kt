package com.bomber.swan.trace.trace

import android.os.Handler
import android.os.SystemClock
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.util.globalHandler

/**
 * @author youngtr
 * @data 2022/5/21
 */
class LooperAnrTracer : Tracer() {

    private val anrHandler by lazy { Handler(globalHandler.looper) }
    private val lagHandler by lazy { Handler(globalHandler.looper) }
    private val anrTask by lazy { Runnable { } }

    override fun onAlive() {
        super.onAlive()

    }

    override fun onDead() {
        super.onDead()
    }


    override fun dispatchEnd(
        beginNs: Long,
        cpuBeginMs: Long,
        endNs: Long,
        cpuEndMs: Long,
        token: Long,
        isVsyncFrame: Boolean
    ) {
        super.dispatchEnd(beginNs, cpuBeginMs, endNs, cpuEndMs, token, isVsyncFrame)
    }

    override fun dispatchBegin(beginNs: Long, cpuBeginNs: Long, token: Long) {
        super.dispatchBegin(beginNs, cpuBeginNs, token)

    }


    companion object {
        private const val TAG = "Swan.AnrTracer"
    }

    internal inner class AnrTask(
        var indexRecord: AppMethodBeat.IndexRecord? = null,
        var token: Long? = null
    ) :
        Runnable {


        override fun run() {
            val current = SystemClock.uptimeMillis()
            val isForeground = isForeground()
            // process

        }

    }

}