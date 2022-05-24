package com.bomber.swan.trace.core

import android.annotation.SuppressLint
import android.os.Looper
import android.os.MessageQueue
import android.util.Printer
import androidx.annotation.CallSuper
import com.bomber.swan.util.SwanLog
import java.util.concurrent.ConcurrentHashMap

class LooperMonitor(val looper: Looper) : MessageQueue.IdleHandler {

    companion object {
        private const val TAG = "Swan.LooperMonitor"

        private val sLooperMonitors = ConcurrentHashMap<Looper, LooperMonitor>()
        private val sMainMonitor = of(Looper.getMainLooper())

        fun of(looper: Looper): LooperMonitor {
            return sLooperMonitors[looper]
                ?: LooperMonitor(looper).apply { sLooperMonitors[looper] = this }
        }
    }

    init {
        resetPrinter()
    }

    private val dispatchListeners by lazy { HashSet<LooperDispatchListener>() }
    private var printer: LooperPrinter? = null

    fun addListener(listener: LooperDispatchListener) {
        synchronized(dispatchListeners) {
            dispatchListeners.add(listener)
        }
    }

    fun removeListener(listener: LooperDispatchListener) {
        synchronized(dispatchListeners) {
            dispatchListeners.remove(listener)
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    @Synchronized
    private fun resetPrinter() {
        var originPrinter: Printer? = null

        try {
            val mLogging =
                looper.javaClass.getDeclaredField("mLogging").apply { isAccessible = true }
            originPrinter = mLogging[looper] as Printer
            if (originPrinter == printer && printer != null) {
                return
            }
        } catch (e: Exception) {
        }

        looper.setMessageLogging(LooperPrinter(originPrinter).apply { printer = this })
    }

    abstract inner class LooperDispatchListener(
        var historyMsgRecord: Boolean = false,
        var denseMsgTracer: Boolean = false
    ) {
        var isHasDispatchStart = false

        fun isValid() = false

        open fun dispatchStart() {}

        @CallSuper
        fun onDispatchStart(x: String) {
            this.isHasDispatchStart = true
            dispatchStart()
        }

        fun onDispatchEnd(x: String) {
            this.isHasDispatchStart = false
            dispatchEnd()
        }

        open fun dispatchEnd() {
        }
    }

    override fun queueIdle(): Boolean {

        return true
    }

    private fun dispatch(isBegin: Boolean, log: String) {

    }


    internal inner class LooperPrinter(private val origin: Printer?) : Printer {

        private var hasChecked: Boolean = false
        private var isValid: Boolean = false

        override fun println(x: String) {
            origin?.println(x)

            if (!hasChecked) {
                isValid = x[0] == '>' || x[0] == '<'
                hasChecked = true
                if (!isValid) {
                    SwanLog.e(TAG, "[println] Printer is invalid! x:%s", x)
                }
            }

            if (isValid) {
                dispatch(x[0] == '>', x)
            }
        }

    }

}