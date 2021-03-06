package com.bomber.swan.resource.matrix.internal

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.SystemClock
import com.bomber.swan.resource.friendly.noOpDelegate
import com.bomber.swan.resource.matrix.EventListener.Event.HeapDump
import com.bomber.swan.resource.matrix.analyzer.AnalysisFailure
import com.bomber.swan.resource.matrix.analyzer.AnalysisSuccess
import com.bomber.swan.resource.matrix.analyzer.AndroidDebugHeapAnalyzer
import com.bomber.swan.resource.matrix.config.ResourceConfig
import com.bomber.swan.resource.matrix.dumper.HeapDumpTrigger
import com.bomber.swan.resource.matrix.watcher.OnObjectRetainedListener
import com.bomber.swan.resource.matrix.watcher.android.AppWatcher
import com.bomber.swan.util.*

/**
 * @author youngtr
 * @data 2022/4/10
 */
@SuppressLint("StaticFieldLeak")
internal object InternalSwanResource : OnObjectRetainedListener {

    private const val TAG = "Swan.Internal"

    private lateinit var heapDumpTrigger: HeapDumpTrigger

    @Suppress("ObjectPropertyName")
    private lateinit var _application: Application

    lateinit var config: ResourceConfig

    val application: Application
        get() {
            check(::_application.isInitialized)
            return _application
        }

    var resumedActivity: Activity? = null

    override fun onObjectRetained() {
        scheduleRetainedObjectCheck()
    }

    private fun scheduleRetainedObjectCheck() {
        if (this::heapDumpTrigger.isInitialized) {
            heapDumpTrigger.scheduleRetainedObjectCheck()
        }
    }

    @Volatile
    var applicationVisible = false
        private set


    fun init(application: Application) {
        _application = application

        AppWatcher.objectWatcher.addOnObjectRetainedListener(this)

        val gcTrigger = GcTrigger.Default

        val configProvider = { this.config }

        val handlerThread = newHandlerThread(LEAK_CANARY_THREAD_NAME)
        val backgroundHandler = Handler(handlerThread.looper)

        // dump process
        heapDumpTrigger = HeapDumpTrigger(
            application, backgroundHandler, AppWatcher.objectWatcher, gcTrigger,
            configProvider, clock = { SystemClock.uptimeMillis() }
        )

        application.registerVisibilityListener { applicationVisible ->
            this.applicationVisible = applicationVisible
            heapDumpTrigger.onApplicationVisibilityChanged(applicationVisible)
        }

        registerResumedActivityListener(application)

    }


    fun createLeakDirectoryProvider(context: Context): LeakDirectoryProvider {
        val appContext = context.applicationContext
        return LeakDirectoryProvider(appContext, {
            this.config.maxStoredHeapDumps
        })
    }

    private fun registerResumedActivityListener(application: Application) {
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks by noOpDelegate() {
            override fun onActivityResumed(activity: Activity) {
                resumedActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                if (resumedActivity == activity) {
                    resumedActivity = null
                }
            }
        })
    }

    fun analyzeHeap(heapDump: HeapDump) {
        SwanLog.d(TAG, "analyzeHeap: ${heapDump.file}")
        globalHandler.post {
            val analysis =
                AndroidDebugHeapAnalyzer.runAnalysisBlocking(heapDump) { process ->
                    SwanLog.d(TAG, "process: ${process.progressPercent}")
                }
            when (analysis) {
                is AnalysisFailure -> {
                    SwanLog.d(TAG, "run analysis fail, need retry?")
                }
                is AnalysisSuccess -> {
                    val resultCallback = this.config.resultCallback
                    resultCallback.report(
                        heapDump.file.absolutePath,
                        heapDump.jsonFile.absolutePath,
                        analysis.result
                    )
                }
            }
        }
    }

    private const val LEAK_CANARY_THREAD_NAME = "Resource-Heap-Dump"

}