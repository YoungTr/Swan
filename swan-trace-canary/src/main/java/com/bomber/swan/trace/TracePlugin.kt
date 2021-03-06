package com.bomber.swan.trace

import android.app.Application
import android.os.Build
import com.bomber.swan.plugin.Plugin
import com.bomber.swan.plugin.PluginListener
import com.bomber.swan.trace.config.TraceConfig
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.trace.core.UIThreadMonitor
import com.bomber.swan.trace.trace.EvilMethodTracer
import com.bomber.swan.trace.trace.LooperAnrTracer
import com.bomber.swan.trace.trace.SignalAnrTracer
import com.bomber.swan.util.SwanLog

class TracePlugin(private val traceConfig: TraceConfig) : Plugin() {

    var isSupported = true
        private set
    private var supportFrameMetrics = false

    private lateinit var looperAnrTracer: LooperAnrTracer
    private lateinit var evilMethodTracer: EvilMethodTracer
    private lateinit var signalAnrTracer: SignalAnrTracer


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

        looperAnrTracer = LooperAnrTracer(traceConfig)
        evilMethodTracer = EvilMethodTracer(traceConfig)
        signalAnrTracer = SignalAnrTracer(traceConfig)

    }


    override fun start() {
        super.start()
        if (!isSupported) {
            SwanLog.w(TAG, "[start] plugin is unSupported!")
            return
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

        if (traceConfig.isEvilMethodTraceEnable) {
            evilMethodTracer.onStartTrace()
        }

        if (traceConfig.isSignalAnrTraceEnable) {
            signalAnrTracer.onStartTrace()
        }

    }

    override fun stop() {
        super.stop()
        looperAnrTracer.onCloseTrace()
        evilMethodTracer.onCloseTrace()
        signalAnrTracer.onCloseTrace()
    }

    private fun TraceConfig.willUiThreadMonitorRunning(): Boolean {
        return this.isEvilMethodTraceEnable || this.isAnrTraceEnable || this.isFPSEnable
    }

    companion object {
        private const val TAG = "Swan.TracePlugin"
    }

}