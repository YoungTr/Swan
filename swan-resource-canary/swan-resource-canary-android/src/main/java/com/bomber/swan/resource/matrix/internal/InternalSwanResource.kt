package com.bomber.swan.resource.matrix.internal

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.SystemClock
import com.bomber.swan.resource.friendly.noOpDelegate
import com.bomber.swan.resource.matrix.ResourceMatrixPlugin
import com.bomber.swan.resource.matrix.dumper.HeapDumpTrigger
import com.bomber.swan.resource.matrix.watcher.OnObjectRetainedListener
import com.bomber.swan.resource.matrix.watcher.android.AppWatcher
import com.bomber.swan.util.GcTrigger
import com.bomber.swan.util.newHandlerThread
import com.bomber.swan.util.registerVisibilityListener

/**
 * @author youngtr
 * @data 2022/4/10
 */
@SuppressLint("StaticFieldLeak")
internal object InternalSwanResource : OnObjectRetainedListener {

    private lateinit var heapDumpTrigger: HeapDumpTrigger

    @Suppress("ObjectPropertyName")
    private lateinit var _application: Application

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

        checkRunningInDebuggableBuild()

        AppWatcher.objectWatcher.addOnObjectRetainedListener(this)

        val gcTrigger = GcTrigger.Default

        val configProvider = { ResourceMatrixPlugin.config }

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
            ResourceMatrixPlugin.config.maxStoredHeapDumps
        }, {
            ResourceMatrixPlugin.config.requestWriteExternalStoragePermission
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

    private fun checkRunningInDebuggableBuild() {

    }

    private const val LEAK_CANARY_THREAD_NAME = "Resource-Heap-Dump"

}