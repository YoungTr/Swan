package com.bomber.swan.trace

import android.app.Application
import android.os.Build
import com.bomber.swan.plugin.Plugin
import com.bomber.swan.plugin.PluginListener
import com.bomber.swan.trace.config.TraceConfig
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.trace.core.UIThreadMonitor
import com.bomber.swan.trace.trace.LooperAnrTracer
import com.bomber.swan.util.SwanLog

class TracePlugin(private val traceConfig: TraceConfig) : Plugin() {

    var isSupported = false
        private set
    private var supportFrameMetrics = false

    private lateinit var looperAnrTracer: LooperAnrTracer


    override fun init(application: Application, pluginListener: PluginListener) {
        super.init(application, pluginListener)
        SwanLog.i(TAG, "trace plugin init, trace config: %s", traceConfig.toString())
        val sdkInt = Build.VERSION.SDK_INT
        if (sdkInt < Build.VERSION_CODES.JELLY_BEAN) {
            SwanLog.e(
                TAG,
                "[FrameBeat] API is low Build.VERSION_CODES.JELLY_BEAN(16), TracePlugin is not supported"
            )
            isSupported = false
            return
        } else if (sdkInt >= Build.VERSION_CODES.O) {
            supportFrameMetrics = true
        }

        if (traceConfig.willUiThreadMonitorRunning()) {
            if (!UIThreadMonitor.isInit) {
                UIThreadMonitor.init(traceConfig, supportFrameMetrics)
            }
        }

        if (traceConfig.isAppMethodBeatEnable) {
            AppMethodBeat.getInstance().onStart()
        } else {
            AppMethodBeat.getInstance().onStop()
        }

        UIThreadMonitor.onStart()

        if (traceConfig.isAnrTraceEnable) {
            looperAnrTracer.onStartTrace()
        }

    }


    override fun start() {
        super.start()
        looperAnrTracer.onStartTrace()

    }

    override fun stop() {
        super.stop()
        looperAnrTracer.onCloseTrace()
    }

    private fun TraceConfig.willUiThreadMonitorRunning(): Boolean {
        return this.isEvilMethodTraceEnable || this.isAnrTraceEnable || this.isFPSEnable
    }

    companion object {
        private const val TAG = "Swan.TracePlugin"
    }

}