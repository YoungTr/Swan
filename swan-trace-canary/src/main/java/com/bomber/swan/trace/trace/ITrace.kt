package com.bomber.swan.trace.trace

import com.bomber.swan.listeners.IAppForeground

/**
 * @author youngtr
 * @data 2022/5/21
 */
interface ITrace : IAppForeground {
    var isAlive: Boolean
    fun onStartTrace()
    fun onCloseTrace()
}