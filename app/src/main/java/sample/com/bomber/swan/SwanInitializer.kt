package sample.com.bomber.swan

import android.app.Application
import android.content.Context
import com.bomber.swan.Swan
import com.bomber.swan.plugin.Plugin
import com.bomber.swan.resource.matrix.ResourceMatrixPlugin
import com.bomber.swan.resource.matrix.dumper.DumpHeapMode
import com.bomber.swan.trace.TracePlugin
import com.bomber.swan.trace.config.TraceConfig
import com.bomber.swan.util.SwanLog
import sample.com.bomber.swan.config.DynamicConfigImplDemo
import sample.com.bomber.swan.listeners.TestPluginListener
import java.io.File

object SwanInitializer {
    private const val TAG = "Swan.SwanInitializer"

    fun init(application: Application) {
        val dynamicConfig = DynamicConfigImplDemo()

        val plugins = HashSet<Plugin>()
        plugins.add(configureTracePlugin(application, dynamicConfig))
        plugins.add(configureResourcePlugin(dynamicConfig))

        val swanBuilder = Swan.SwanBuilder(application, plugins, TestPluginListener(application))
        Swan.init(swanBuilder.build())
        Swan.with().startAllPlugins()

    }


    private fun configureTracePlugin(
        context: Context,
        dynamicConfig: DynamicConfigImplDemo
    ): TracePlugin {
        val fpsEnable = dynamicConfig.isFPSEnable
        val traceEnable = dynamicConfig.isTraceEnable
        val signalAnrTraceEnable = dynamicConfig.isSignalAnrTraceEnable
        val traceFileDir: File = File(context.filesDir, "swan_trace")
        if (!traceFileDir.exists()) {
            if (traceFileDir.mkdirs()) {
                SwanLog.e(TAG, "failed to create traceFileDir")
            }
        }
        val anrTraceFile = File(
            traceFileDir,
            "anr_trace"
        ) // path : /data/user/0/sample.tencent.matrix/files/matrix_trace/anr_trace
        val printTraceFile = File(
            traceFileDir,
            "print_trace"
        ) // path : /data/user/0/sample.tencent.matrix/files/matrix_trace/print_trace
        val traceConfig: TraceConfig = TraceConfig.Builder()
            .dynamicConfig(dynamicConfig)
            .enableFPS(fpsEnable)
            .enableEvilMethodTrace(traceEnable)
            .enableAnrTrace(traceEnable)
            .enableStartup(traceEnable)
            .enableIdleHandlerTrace(traceEnable) // Introduced in Matrix 2.0
            .enableMainThreadPriorityTrace(true) // Introduced in Matrix 2.0
            .enableSignalAnrTrace(signalAnrTraceEnable) // Introduced in Matrix 2.0
            .anrTracePath(anrTraceFile.absolutePath)
            .printTracePath(printTraceFile.absolutePath)
            .splashActivities("sample.tencent.matrix.SplashActivity;")
            .isDebug(true)
            .isDevEnv(false)
            .build()

        //Another way to use SignalAnrTracer separately
        //useSignalAnrTraceAlone(anrTraceFile.getAbsolutePath(), printTraceFile.getAbsolutePath());
        return TracePlugin(traceConfig)
    }

    private fun configureResourcePlugin(dynamicConfig: DynamicConfigImplDemo): ResourceMatrixPlugin {

        return ResourceMatrixPlugin().apply {
            this.config =
                config.copy(retainedVisibleThreshold = 1, dumpHeapMode = DumpHeapMode.NormalDump)

        }
    }

}