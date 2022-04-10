package com.bomber.swan.resource.matrix

import android.app.Application
import com.bomber.swan.plugin.Plugin
import com.bomber.swan.plugin.PluginListener
import com.bomber.swan.resource.matrix.watcher.android.AppWatcher

/**
 * @author youngtr
 * @data 2022/4/9
 */
class ResourceMatrixPlugin : Plugin() {


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
}