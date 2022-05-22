package com.bomber.swan.trace.trace

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.bomber.swan.trace.constants.TARGET_EVIL_METHOD_STACK
import com.bomber.swan.trace.constants.TIME_UPDATE_CYCLE_MS
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.trace.items.MethodItem
import com.bomber.swan.trace.util.IStructuredDataFilter
import com.bomber.swan.trace.util.TraceDataMarker
import com.bomber.swan.util.SystemInfo
import com.bomber.swan.util.getStack
import com.bomber.swan.util.globalHandler
import java.util.*

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
        var beginRecord: AppMethodBeat.IndexRecord? = null,
        var token: Long? = null
    ) :
        Runnable {


        override fun run() {
            val current = SystemClock.uptimeMillis()
            val isForeground = isForeground()
            // process & memory
            SystemInfo.refresh()
            val procStat = SystemInfo.procStat
            val data = AppMethodBeat.getInstance().copyData(beginRecord)
            beginRecord?.release()

            val status = Looper.getMainLooper().thread.state
            val stackTrace = Looper.getMainLooper().thread.stackTrace
            val dumpStack = getStack(stackTrace, "|*\t\t", start = 0)
            val stack = LinkedList<MethodItem>()
            if (data.isNotEmpty()) {
                TraceDataMarker.structuredDataToStack(data, stack, true, current)
                TraceDataMarker.trimStack(
                    stack,
                    TARGET_EVIL_METHOD_STACK,
                    object : IStructuredDataFilter {
                        override fun isFilter(during: Long, filterCount: Int): Boolean {
                            return during < filterCount * TIME_UPDATE_CYCLE_MS
                        }

                        override fun filterMaxCount(): Int {
                            TODO("Not yet implemented")
                        }

                        override fun fallback(stack: List<MethodItem>, size: Int) {
                            TODO("Not yet implemented")
                        }

                    })
            }
        }

    }

}