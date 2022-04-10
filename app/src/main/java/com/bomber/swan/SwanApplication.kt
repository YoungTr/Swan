package com.bomber.swan

import android.app.Application
import com.bomber.swan.plugin.DefaultPluginListener
import com.bomber.swan.resource.matrix.ResourceMatrixPlugin

/**
 * @author youngtr
 * @data 2022/4/10
 */
class SwanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val plugin = ResourceMatrixPlugin()
        plugin.init(this, DefaultPluginListener)
        plugin.start()
    }
}