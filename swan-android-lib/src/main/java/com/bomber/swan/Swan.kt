package com.bomber.swan

import android.app.Application
import com.bomber.swan.plugin.DefaultPluginListener
import com.bomber.swan.plugin.Plugin
import com.bomber.swan.plugin.PluginListener

class Swan private constructor(
    val application: Application,
    val plugins: Set<Plugin>,
    private val pluginListener: PluginListener
) {
    companion object {
        private lateinit var instance: Swan

        @JvmStatic
        fun isInstalled() = ::instance.isInitialized

        @JvmStatic
        fun init(swan: Swan): Swan {
            if (!isInstalled()) this.instance = swan
            return instance
        }

        @JvmStatic
        fun with(): Swan {
            if (!isInstalled()) {
                throw RuntimeException("you must init Swan sdk first")
            }
            return instance
        }
    }

    init {
        plugins.forEach {
            it.init(application, pluginListener)
        }
    }

    fun startAllPlugins() {
        plugins.forEach { it.start() }
    }

    fun stopAllPlugins() {
        plugins.forEach { it.stop() }
    }

    fun destroyAllPlugins() {
        plugins.forEach { it.destroy() }
    }

    fun <T : Plugin> getPlugin(plugin: Class<T>): T? {
        val name = plugin.name
        plugins.forEach {
            if (it.javaClass.name.equals(name)) {
                return it as T
            }
        }
        return null
    }

    class SwanBuilder(
        val application: Application,
        val plugins: Set<Plugin>,
        val pluginListener: PluginListener = DefaultPluginListener(),
    ) {

        fun build(): Swan {
            return Swan(application, plugins, pluginListener)
        }
    }

}


