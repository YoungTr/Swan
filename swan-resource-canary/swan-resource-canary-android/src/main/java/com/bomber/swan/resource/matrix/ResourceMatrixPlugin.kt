package com.bomber.swan.resource.matrix

import android.app.Application
import android.content.Intent
import com.bomber.swan.plugin.Plugin
import com.bomber.swan.plugin.PluginListener
import com.bomber.swan.resource.matrix.config.ResourceConfig
import com.bomber.swan.resource.matrix.watcher.android.AppWatcher
import com.bomber.swan.util.SwanLog

/**
 * @author youngtr
 * @data 2022/4/9
 */
object ResourceMatrixPlugin : Plugin() {

    @Volatile
    var config: ResourceConfig = ResourceConfig()
        set(newConfig) {
            val previousConfig = field
            field = newConfig
            logConfigChange(previousConfig, newConfig)
//            HeapDumpControl.updateICanHasHeap()
        }

    override fun init(application: Application, pluginListener: PluginListener) {
        super.init(application, pluginListener)
        AppWatcher.init(application)
    }

    override fun start() {
        super.start()
        AppWatcher.start()
    }

    override fun stop() {
        super.stop()
    }

    override fun destroy() {
        super.destroy()
        AppWatcher.destroy()
    }

    /**
     * Returns a new [Intent] that can be used to programmatically launch the leak display activity.
     */
//    fun newLeakDisplayActivityIntent() =
//        LeakActivity.createHomeIntent(InternalLeakCanary.application)

    /**
     * Dynamically shows / hides the launcher icon for the leak display activity.
     * Note: you can change the default value by overriding the `leak_canary_add_launcher_icon`
     * boolean resource:
     *
     * ```xml
     * <?xml version="1.0" encoding="utf-8"?>
     * <resources>
     *   <bool name="leak_canary_add_launcher_icon">false</bool>
     * </resources>
     * ```
     */
//    fun showLeakDisplayActivityLauncherIcon(showLauncherIcon: Boolean) {
//        InternalLeakCanary.setEnabledBlocking(
//            "leakcanary.internal.activity.LeakLauncherActivity", showLauncherIcon
//        )
//    }

    /**
     * Immediately triggers a heap dump and analysis, if there is at least one retained instance
     * tracked by [AppWatcher.objectWatcher]. If there are no retained instances then the heap will not
     * be dumped and a notification will be shown instead.
     */
//    fun dumpHeap() = InternalLeakCanary.onDumpHeapReceived(forceDump = true)

    private fun logConfigChange(
        previousConfig: ResourceConfig,
        newConfig: ResourceConfig
    ) {
        SwanLog.d("Swan.SwanResource") {
            val changedFields = mutableListOf<String>()
            ResourceConfig::class.java.declaredFields.forEach { field ->
                field.isAccessible = true
                val previousValue = field[previousConfig]
                val newValue = field[newConfig]
                if (previousValue != newValue) {
                    changedFields += "${field.name}=$newValue"
                }
            }
            val changesInConfig =
                if (changedFields.isNotEmpty()) changedFields.joinToString(", ") else "no changes"

            "Updated LeakCanary.config: Config($changesInConfig)"
        }
    }
}