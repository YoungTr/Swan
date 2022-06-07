@file:Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")

package com.bomber.swan.trace.core

import android.os.SystemClock
import android.view.Choreographer
import com.bomber.swan.trace.config.TraceConfig
import com.bomber.swan.trace.constants.DEFAULT_FRAME_DURATION
import com.bomber.swan.trace.constants.TIME_MILLIS_TO_NANO
import com.bomber.swan.trace.friendly.checkMainThread
import com.bomber.swan.trace.listeners.LooperObserver
import com.bomber.swan.util.ReflectUtils
import com.bomber.swan.util.SwanLog
import com.bomber.swan.util.getStack
import java.lang.reflect.Method

object UIThreadMonitor : BeatLifecycle, Runnable {

    private const val TAG = "Swan.UIThreadMonitor"
    private const val ADD_CALLBACK = "addCallbackLocked"
    private const val OLDEST_INPUT_EVENT = 3
    private const val NEWEST_INPUT_EVENT = 4

    const val CALLBACK_INPUT = 0
    const val CALLBACK_ANIMATION = 1
    const val CALLBACK_TRAVERSAL = 2

    const val DO_QUEUE_END_ERROR = -100L

    private const val CALLBACK_LAST = CALLBACK_TRAVERSAL

    private val dispatchTimeMs = LongArray(4)
    private val observers = hashSetOf<LooperObserver>()
    private var isVsyncFrame = false

    @Volatile
    private var token = 0L


    private lateinit var config: TraceConfig
    private var useFrameMetrics = false

    private val callbackQueueLock: Any by lazy {
        ReflectUtils.reflectObject(
            choreographer,
            "mCallbackQueues",
            null
        )!!
    }

    private val callbackQueues: Array<Any>? by lazy {
        ReflectUtils.reflectObject(choreographer, "mCallbackQueue", null)
    }
    private val addTraversalQueue: Method? by lazy {
        callbackQueues?.let {
            ReflectUtils.reflectMethod(
                it[CALLBACK_TRAVERSAL],
                ADD_CALLBACK, Long::class.java, Any::class.java, Any::class.java
            )
        }
    }
    private val addInputQueue: Method? by lazy {
        callbackQueues?.let {
            ReflectUtils.reflectMethod(
                it[CALLBACK_INPUT],
                ADD_CALLBACK, Long::class.java, Any::class.java, Any::class.java
            )
        }
    }
    private val addAnimationQueue: Method? by lazy {
        callbackQueues?.let {
            ReflectUtils.reflectMethod(
                it[CALLBACK_ANIMATION],
                ADD_CALLBACK, Long::class.java, Any::class.java, Any::class.java
            )
        }
    }
    private val choreographer: Choreographer by lazy { Choreographer.getInstance() }

    private val vsyncReceiver by lazy {
        @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
        ReflectUtils.reflectObject(
            choreographer,
            "mDisplayEventReceiver",
            null
        )
    }
    private val frameIntervalNanos: Long by lazy {
        ReflectUtils.reflectObject(
            choreographer, "mFrameIntervalNanos",
            DEFAULT_FRAME_DURATION
        )!!
    }
    private var queueStatus = IntArray(CALLBACK_LAST + 1)
    private var callbackExist = BooleanArray(CALLBACK_LAST + 1) // ABA
    private var queueCost = LongArray(CALLBACK_LAST + 1)
    private const val DO_QUEUE_DEFAULT = 0
    private const val DO_QUEUE_BEGIN = 1
    private const val DO_QUEUE_END = 2
    var isInit = false
        private set

    @Volatile
    private var isAlive = false

    fun init(config: TraceConfig, supportFrameMetrics: Boolean) {
        this.config = config
        useFrameMetrics = supportFrameMetrics

        checkMainThread()

        val historyMsgRecorder = config.historyMsgRecorder
        val denseMsgTracer = config.denseMsgTracer

        LooperMonitor.register(
            object : LooperMonitor.LooperDispatchListener(
                historyMsgRecorder,
                denseMsgTracer
            ) {
                override fun isValid(): Boolean {
                    return isAlive
                }

                override fun dispatchStart() {
                    super.dispatchStart()
                    this@UIThreadMonitor.dispatchBegin()
                }

                override fun dispatchEnd() {
                    super.dispatchEnd()
                    this@UIThreadMonitor.dispatchEnd()
                }
            }
        )
        isInit = true
        if (!useFrameMetrics) {
            if (config.isDevEnv) {
                addObserver(object : LooperObserver() {
                    override fun doFrame(
                        focusedActivity: String?,
                        startNs: Long,
                        endNs: Long,
                        isVsyncFrame: Boolean,
                        intendedFrameTimeNs: Long,
                        inputCostNs: Long,
                        animationCostNs: Long,
                        traversalCostNs: Long
                    ) {
                        SwanLog.i(
                            TAG,
                            "focusedActivity[%s] frame cost:%sms isVsyncFrame=%s intendedFrameTimeNs=%s [%s|%s|%s]ns",
                            focusedActivity,
                            (endNs - startNs) / TIME_MILLIS_TO_NANO,
                            isVsyncFrame,
                            intendedFrameTimeNs,
                            inputCostNs,
                            animationCostNs,
                            traversalCostNs
                        )

                    }
                })
            }
        }

    }

    override fun onStart() {
        if (!isInit) {
            SwanLog.e(TAG, "[onStart] is never init.")
            return
        }

        if (!isAlive) {
            this.isAlive = true
            synchronized(this) {
                SwanLog.i(
                    TAG,
                    "[onStart] callbackExist:%s %s",
                    callbackExist.contentToString(),
                    getStack()
                )
                callbackExist = BooleanArray(CALLBACK_LAST + 1)
            }
            if (!useFrameMetrics) {
                queueStatus = IntArray(CALLBACK_LAST + 1)
                queueCost = LongArray(CALLBACK_LAST + 1)
                addFrameCallback(CALLBACK_INPUT, this, true)
            }
        }
    }

    @Synchronized
    private fun addFrameCallback(type: Int, callback: Runnable, isAddHeader: Boolean) {
        if (callbackExist[type]) {
            SwanLog.w(
                TAG,
                "[addFrameCallback] this type %s callback has exist! isAddHeader:%s",
                type,
                isAddHeader
            )
            return
        }
        if (!isAlive && type == CALLBACK_INPUT) {
            SwanLog.w(TAG, "[addFrameCallback] UIThreadMonitor is not alive!")
            return
        }

        try {
            synchronized(callbackQueueLock) {
                val method: Method? = when (type) {
                    CALLBACK_INPUT -> addInputQueue
                    CALLBACK_ANIMATION -> addAnimationQueue
                    CALLBACK_TRAVERSAL -> addTraversalQueue
                    else -> null
                }
                method?.let {
                    it.invoke(
                        callbackQueues!![type],
                        if (isAddHeader) -1 else SystemClock.uptimeMillis(),
                        callback,
                        null
                    )
                    callbackExist[type] = true
                }
            }

        } catch (e: Exception) {
            SwanLog.e(TAG, e.toString())
        }

    }

    override fun onStop() {
        if (!isInit) {
            SwanLog.e(TAG, "[onStart] is never init.")
            return
        }
        if (isAlive) {
            this.isAlive = false
            SwanLog.i(
                TAG,
                "[onStop] callbackExist:%s %s",
                callbackExist.contentToString(),
                getStack()
            )

        }
    }

    override fun isAlive(): Boolean {
        return isAlive
    }

    override fun run() {
        val start = System.nanoTime()
        try {
            doFrameBegin(token)

            addFrameCallback(CALLBACK_ANIMATION, {
                doQueueEnd(CALLBACK_INPUT)
                doQueueBegin(CALLBACK_ANIMATION)
            }, true)

            addFrameCallback(CALLBACK_TRAVERSAL, {
                doQueueEnd(CALLBACK_ANIMATION)
                doQueueBegin(CALLBACK_TRAVERSAL)
            }, true)

        } finally {
            if (config.isDevEnv()) {
                SwanLog.d(TAG, "[UIThreadMonitor#run] inner cost:%sns", System.nanoTime() - start)
            }
        }
    }


    private fun doFrameBegin(token: Long) {
        this.isVsyncFrame = true
    }

    private fun doFrameEnd(token: Long) {
        doQueueEnd(CALLBACK_TRAVERSAL)
        for (i in queueStatus) {
            queueCost[i] = DO_QUEUE_END_ERROR
            if (config.isDevEnv) {
                throw RuntimeException(
                    String.format(
                        "UIThreadMonitor happens type[%s] != DO_QUEUE_END",
                        i
                    )
                )
            }
        }
        queueStatus = IntArray(CALLBACK_LAST + 1)
        addFrameCallback(CALLBACK_INPUT, this, true)
    }

    private fun doQueueBegin(type: Int) {
        queueStatus[type] = DO_QUEUE_BEGIN
        queueCost[type] = System.nanoTime()
    }

    private fun doQueueEnd(type: Int) {
        queueStatus[type] = DO_QUEUE_END
        queueCost[type] = System.nanoTime() - queueCost[type]
        synchronized(this) { callbackExist[type] = false }
    }


    private fun dispatchBegin() {
        token = System.nanoTime().apply { dispatchTimeMs[0] = this }
        dispatchTimeMs[2] = SystemClock.currentThreadTimeMillis()
        if (config.isAppMethodBeatEnable) {
            AppMethodBeat.i(AppMethodBeat.METHOD_ID_DISPATCH)
        }
        synchronized(observers) {
            observers.forEach {
                if (!it.isDispatchBegin()) {
                    it.dispatchBegin(dispatchTimeMs[0], dispatchTimeMs[2], token)
                }
            }
        }

    }

    private fun dispatchEnd() {
        var traceBegin = 0L
        if (config.isDevEnv) {
            traceBegin = System.nanoTime()
        }
        if (config.isFPSEnable && !useFrameMetrics) {
            val startNs = token
            var intendedFrameTimeNs = startNs
            if (isVsyncFrame) {
                doFrameEnd(token)
                intendedFrameTimeNs = getIntendedFrameTimeNs(startNs)
            }

            val endNs = System.nanoTime()
            synchronized(observers) {
                observers.forEach {
                    if (it.isDispatchBegin()) {
                        it.doFrame(
                            "todo",
                            startNs,
                            endNs,
                            isVsyncFrame,
                            intendedFrameTimeNs,
                            queueCost[CALLBACK_INPUT],
                            queueCost[CALLBACK_ANIMATION],
                            queueCost[CALLBACK_TRAVERSAL]
                        )
                    }
                }
            }
        }

        if (config.isEvilMethodTraceEnable || config.isDevEnv) {
            dispatchTimeMs[3] = SystemClock.currentThreadTimeMillis()
            dispatchTimeMs[1] = System.nanoTime()
        }
        AppMethodBeat.o(AppMethodBeat.METHOD_ID_DISPATCH)
        synchronized(observers) {
            observers.forEach {
                if (it.isDispatchBegin()) {
                    it.dispatchEnd(
                        dispatchTimeMs[0], dispatchTimeMs[2], dispatchTimeMs[1],
                        dispatchTimeMs[3], token, isVsyncFrame
                    )
                }
            }
        }
        this.isVsyncFrame = false
        if (config.isDevEnv) {
            SwanLog.d(
                TAG,
                "[dispatchEnd#run] inner cost:%sns",
                System.nanoTime() - traceBegin
            )

        }

    }

    private fun getIntendedFrameTimeNs(defaultValue: Long): Long {
        return ReflectUtils.reflectObject(vsyncReceiver, "mTimestampNanos", defaultValue)!!
    }


    @JvmStatic
    fun addObserver(observer: LooperObserver) {
        if (!isAlive) {
            onStart()
        }
        synchronized(observers) {
            observers.add(observer)
        }
    }

    @JvmStatic
    fun removeObserver(observer: LooperObserver) {
        synchronized(observers) {
            observers.apply {
                remove(observer)
                if (isEmpty()) onStop()
            }

        }
    }

    fun getQueueCost(type: Int, token: Long): Long {
        if (token != this.token) {
            return -1
        }
        return if (queueStatus[type] == DO_QUEUE_END) queueCost[type] else 0
    }
}