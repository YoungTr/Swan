package com.bomber.swan.trace

import android.app.Application
import com.bomber.swan.plugin.Plugin
import com.bomber.swan.plugin.PluginListener
import com.bomber.swan.trace.trace.LooperAnrTracer

class TracePlugin : Plugin() {

    private lateinit var looperAnrTracer: LooperAnrTracer

    override fun init(application: Application, pluginListener: PluginListener) {
        super.init(application, pluginListener)

        looperAnrTracer = LooperAnrTracer()


    }


    override fun start() {
        super.start()
        looperAnrTracer.onStartTrace()

    }

    override fun stop() {
        super.stop()
        looperAnrTracer.onCloseTrace()
    }

}