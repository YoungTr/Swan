package com.bomber.swan.trace.trace

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.SyncStateContract
import com.bomber.swan.trace.constants.DEFAULT_ANR
import com.bomber.swan.trace.constants.FILTER_STACK_MAX_COUNT
import com.bomber.swan.trace.constants.TARGET_EVIL_METHOD_STACK
import com.bomber.swan.trace.constants.TIME_UPDATE_CYCLE_MS
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.trace.items.MethodItem
import com.bomber.swan.trace.util.IStructuredDataFilter
import com.bomber.swan.trace.util.TraceDataMarker
import com.bomber.swan.util.SwanLog
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

            val stackKey = TraceDataMarker.geTreeKey(stack, stackCost)
            SwanLog.w(
                TAG, "%s \npostTime:%s curTime:%s",
                printAnr(
                    scene,
                    processStat,
                    memoryInfo,
                    status,
                    logcatBuilder,
                    isForeground,
                    stack.size.toLong(),
                    stackKey,
                    dumpStack,
                    inputCost,
                    animationCost,
                    traversalCost,
                    stackCost
                ),
                token / SyncStateContract.Constants.TIME_MILLIS_TO_NANO, curTime
            ) // for logcat


        }

        private fun printAnr(
            scene: String,
            processStat: IntArray,
            memoryInfo: LongArray,
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
        ): String? {
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
            print.append("|*\t\tPriority: ").append(processStat[0]).append("\tNice: ")
                .append(processStat[1]).append("\n")
            print.append("|*\t\tis64BitRuntime: ").append(DeviceUtil.is64BitRuntime()).append("\n")
            print.append("|* [Memory]").append("\n")
            print.append("|*\t\tDalvikHeap: ").append(memoryInfo[0]).append("kb\n")
            print.append("|*\t\tNativeHeap: ").append(memoryInfo[1]).append("kb\n")
            print.append("|*\t\tVmSize: ").append(memoryInfo[2]).append("kb\n")
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
                    java.lang.String.format(
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

}