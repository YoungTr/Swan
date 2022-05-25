package com.bomber.swan.trace.core

import android.annotation.SuppressLint
import android.os.Build
import android.os.Looper
import android.os.MessageQueue
import android.os.SystemClock
import android.util.Printer
import androidx.annotation.CallSuper
import com.bomber.swan.util.SwanLog
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("DiscouragedPrivateApi")
class LooperMonitor(private val looper: Looper) : MessageQueue.IdleHandler {

    companion object {
        private const val TAG = "Swan.LooperMonitor"

        private val sLooperMonitors = ConcurrentHashMap<Looper, LooperMonitor>()
        private val sMainMonitor = of(Looper.getMainLooper())

        private const val CHECK_TIME = 60 * 1000L

        fun of(looper: Looper): LooperMonitor {
            return sLooperMonitors[looper]
                ?: LooperMonitor(looper).apply { sLooperMonitors[looper] = this }
        }

        @JvmStatic
        fun register(listener: LooperDispatchListener) {
            sMainMonitor.addListener(listener)
        }

        @JvmStatic
        fun unregister(listener: LooperDispatchListener) {
            sMainMonitor.removeListener(listener)
        }
    }

    init {
        resetPrinter()
        addIdleHandler(looper)
    }


    private val mQueue by lazy {
        looper.javaClass.getDeclaredField("mQueue").apply { isAccessible = true }
    }

    @SuppressLint("DiscouragedPrivateApi", "ObsoleteSdkInt")
    @Synchronized
    private fun addIdleHandler(looper: Looper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            looper.queue.addIdleHandler(this)
        } else {
            try {
                val queue = mQueue[looper] as MessageQueue
                queue.addIdleHandler(this)
            } catch (e: Exception) {
                SwanLog.e(TAG, "[addIdleHandler] %s", e)
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    @Synchronized
    private fun removeIdleHandler(looper: Looper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            looper.queue.removeIdleHandler(this)
        } else {
            try {
                val queue = mQueue[looper] as MessageQueue
                queue.removeIdleHandler(this)
            } catch (e: Exception) {
                SwanLog.e(TAG, "[removeIdleHandler] %s", e)
            }
        }
    }


    private val dispatchListeners by lazy { HashSet<LooperDispatchListener>() }
    private var printer: LooperPrinter? = null
    private var lastCheckPrinterTime = 0L


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
            originPrinter = mLogging[looper] as Printer?
            if (originPrinter == printer && printer != null) {
                return
            }

            if (originPrinter != null && printer != null) {
                if (originPrinter.javaClass.name.equals(printer!!.javaClass.name)) {
                    SwanLog.w(
                        TAG, "LooperPrinter might be loaded by different classloader"
                                + ", my = " + printer!!.javaClass.classLoader
                                + ", other = " + originPrinter.javaClass.classLoader
                    )
                    return
                }
            }

        } catch (e: Exception) {
            SwanLog.e(TAG, "${e.message}")
        }

        if (null != printer) {
            SwanLog.w(
                TAG, "maybe thread:%s printer[%s] was replace other[%s]!",
                looper.thread.name, printer, originPrinter
            )
        }

        looper.setMessageLogging(LooperPrinter(originPrinter).apply { printer = this })
        if (null != originPrinter) {
            SwanLog.i(
                TAG,
                "reset printer, originPrinter[%s] in %s",
                originPrinter,
                looper.thread.name
            )
        }
    }

    open class LooperDispatchListener
    @JvmOverloads
    constructor(
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
        if (SystemClock.uptimeMillis() - lastCheckPrinterTime >= CHECK_TIME) {
            resetPrinter()
            lastCheckPrinterTime = SystemClock.uptimeMillis()
        }
        return true
    }

    private fun dispatch(isBegin: Boolean, log: String) {
//        SwanLog.d(TAG, "Looper isBegin: $isBegin, log: $log")
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