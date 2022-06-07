package com.bomber.swan.trace.trace

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.bomber.swan.trace.config.TraceConfig
import com.bomber.swan.trace.constants.*
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.trace.core.UIThreadMonitor
import com.bomber.swan.trace.items.MethodItem
import com.bomber.swan.trace.util.IStructuredDataFilter
import com.bomber.swan.trace.util.TraceDataMarker
import com.bomber.swan.util.*
import com.bomber.swan.util.SystemInfo.calculateCpuUsage
import java.util.*

/**
 * @author youngtr
 * @data 2022/5/21
 */
class LooperAnrTracer(private val traceConfig: TraceConfig) : Tracer() {

    private val isAnrTraceEnable = traceConfig.isAnrTraceEnable

    private val anrHandler by lazy { Handler(globalHandler.looper) }
    private val lagHandler by lazy { Handler(globalHandler.looper) }
    private val anrTask by lazy { AnrTask() }

    override fun onAlive() {
        super.onAlive()
        if (isAnrTraceEnable) {
            UIThreadMonitor.addObserver(this)
        }

    }

    override fun onDead() {
        super.onDead()
        if (isAnrTraceEnable) {
            UIThreadMonitor.removeObserver(this)
            anrTask.beginRecord?.release()
            anrHandler.removeCallbacksAndMessages(null)
        }
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
        if (traceConfig.isDevEnv) {
            val cost: Long = (endNs - beginNs) / TIME_MILLIS_TO_NANO
            SwanLog.v(
                TAG, "[dispatchEnd] token:%s cost:%sms cpu:%sms usage:%s",
                token, cost,
                cpuEndMs - cpuBeginMs, calculateCpuUsage(cpuEndMs - cpuBeginMs, cost)
            )
        }
        anrTask.beginRecord?.release()
        anrHandler.removeCallbacks(anrTask)
    }

    override fun dispatchBegin(beginNs: Long, cpuBeginNs: Long, token: Long) {
        super.dispatchBegin(beginNs, cpuBeginNs, token)
        anrTask.beginRecord = AppMethodBeat.getInstance().maskIndex("AnrTrace#dispatchBegin")
        anrTask.token = token
        if (traceConfig.isDevEnv) {
            SwanLog.v(
                TAG,
                "* [dispatchBegin] token:%s index:%s",
                token,
                anrTask.beginRecord!!.index
            )
        }
        val cost = (System.nanoTime() - token) / TIME_MILLIS_TO_NANO
        anrHandler.postDelayed(anrTask, DEFAULT_ANR - cost)

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
                            return FILTER_STACK_MAX_COUNT
                        }

                        override fun fallback(stack: LinkedList<MethodItem>, size: Int) {
                            SwanLog.w(
                                TAG,
                                "[fallback] size: %s, target size: $TARGET_EVIL_METHOD_STACK, stack: $stack"
                            )
                            val iterator =
                                stack.listIterator(size.coerceAtMost(TARGET_EVIL_METHOD_STACK))
                            while (iterator.hasNext()) {
                                iterator.next()
                                iterator.remove()
                            }
                        }

                    })
            }

            val reportBuilder = StringBuilder()
            val logcatBuilder = StringBuilder()
            val stackCost = DEFAULT_ANR.coerceAtLeast(
                TraceDataMarker.stackToString(
                    stack,
                    reportBuilder,
                    logcatBuilder
                )
            )

            // frame
            val inputCost: Long =
                UIThreadMonitor.getQueueCost(UIThreadMonitor.CALLBACK_INPUT, token!!)
            val animationCost: Long =
                UIThreadMonitor.getQueueCost(
                    UIThreadMonitor.CALLBACK_ANIMATION, token!!
                )
            val traversalCost: Long =
                UIThreadMonitor.getQueueCost(UIThreadMonitor.CALLBACK_TRAVERSAL, token!!)

            val stackKey = TraceDataMarker.getTreeKey(stack, stackCost)
            SwanLog.w(
                TAG, "%s \npostTime:%s curTime:%s",
                printAnr(
                    "Activity",
                    SystemInfo.procStat,
                    SystemInfo.procStatus,
                    SystemInfo.memInfo,
                    status,
                    logcatBuilder,
                    isForeground,
                    stack.size.toLong(),
                    stackKey,
                    dumpStack,
                    inputCost,
                    animationCost,
                    traversalCost,
                    stackCost.toLong()
                ),
                token!!.div(TIME_MILLIS_TO_NANO), current
            ) // for logcat


        }

        private fun printAnr(
            scene: String,
            processStat: SystemInfo.ProcStat,
            processStatus: SystemInfo.ProcStatus,
            memoryInfo: SystemInfo.MemInfo,
            state: Thread.State,
            stack: java.lang.StringBuilder,
            isForeground: Boolean,
            stackSize: Long,
            stackKey: String,
            dumpStack: String,
            inputCost: Long,
            animationCost: Long,
            traversalCost: Long,
            stackCost: Long
        ): String {
            val print = java.lang.StringBuilder()
            print.append(
                String.format(
                    "-\n>>>>>>>>>>>>>>>>>>>>>>> maybe happens ANR(%s ms)! <<<<<<<<<<<<<<<<<<<<<<<\n",
                    stackCost
                )
            )
            print.append("|* [Status]").append("\n")
            print.append("|*\t\tScene: ").append(scene).append("\n")
            print.append("|*\t\tForeground: ").append(isForeground).append("\n")
            print.append("|*\t\tPriority: ").append(processStat.priority).append("\tNice: ")
                .append(processStat.nice).append("\n")
            print.append("|*\t\tis64BitRuntime: ").append(DeviceUtil.is64BitRuntime()).append("\n")
            print.append("|* [Memory]").append("\n")
            print.append("|*\t\tDalvikHeap: ").append(memoryInfo.totalInKb - memoryInfo.freeInKb)
                .append("kb\n")
            print.append("|*\t\tNativeHeap: ").append(memoryInfo.nativeHeap).append("\n")
            print.append("|*\t\tVmSize: ").append(processStatus.vssInKb).append("kb\n")
            print.append("|* [doFrame]").append("\n")
            print.append("|*\t\tinputCost:animationCost:traversalCost").append("\n")
            print.append("|*\t\t").append(inputCost).append(":").append(animationCost).append(":")
                .append(traversalCost).append("\n")
            print.append("|* [Thread]").append("\n")
            print.append(String.format("|*\t\tStack(%s): ", state)).append(dumpStack)
            print.append("|* [Trace]").append("\n")
            if (stackSize > 0) {
                print.append("|*\t\tStackKey: ").append(stackKey).append("\n")
                print.append(stack.toString())
            } else {
                print.append(
                    String.format(
                        "AppMethodBeat is close[%s].",
                        AppMethodBeat.getInstance().isAlive()
                    )
                ).append("\n")
            }
            print.append("=========================================================================")
            return print.toString()
        }
    }

}