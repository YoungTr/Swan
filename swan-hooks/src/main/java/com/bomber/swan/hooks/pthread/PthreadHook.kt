package com.bomber.swan.hooks.pthread

import com.bomber.swan.hooks.AbsHook

private class PthreadHook : AbsHook() {


    companion object {
        val TAG = "Swan.Pthread"

        val INSTANCE: PthreadHook = PthreadHook()
    }

    val hookThreadName = HashSet<String>()
    var enableQuicken = false
    var enableLog = false
    var configured = false
    var threadTraceEnable = false
    var hookInstalled = false
    var enableTracePthreadRelease = false


    override fun getNativeLibraryName(): String {
        return "swan-pthreadhook"
    }

    override fun onConfigure(): Boolean {
        TODO("Not yet implemented")
    }

    override fun onHook(enableDebug: Boolean): Boolean {
        TODO("Not yet implemented")
    }
}