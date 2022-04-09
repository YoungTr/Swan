package com.bomber.swan.plugin

import android.app.Application

/**
 * @author youngtr
 * @data 2022/4/9
 */
interface IPlugin {

    fun getApplication(): Application

    fun init(application: Application, pluginListener: PluginListener)

    fun start()

    fun stop()

    fun destroy()

    fun getTag(): String = javaClass.name

    fun onForeground(isForeground: Boolean)
}