package com.bomber.swan.hooks.pthread

import androidx.annotation.Keep
import com.bomber.swan.hooks.AbsHook

class PthreadHook private constructor() : AbsHook() {


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


    fun dump(path: String) {
        if (status == Status.COMMIT_SUCCESS) {
            dumpNative(path)
        }
    }

    override fun onConfigure(): Boolean {
        addHookThreadNameNative(hookThreadName.toArray(Array(0) { "" }))
        enableQuickenNative(enableQuicken)
        enableTracePthreadReleaseNative(enableTracePthreadRelease)
        return true
    }

    override fun onHook(enableDebug: Boolean): Boolean {
        if (!hookInstalled) {
            installHooksNative(enableDebug)
            hookInstalled = true
        }
        return true
    }

    @Keep
    private external fun addHookThreadNameNative(threadNames: Array<String>)

    @Keep
    private external fun enableQuickenNative(enableQuick: Boolean)

    @Keep
    private external fun enableLoggerNative(enableLogger: Boolean)

    @Keep
    private external fun enableTracePthreadReleaseNative(enableTrace: Boolean)

    @Keep
    private external fun installHooksNative(enableDebug: Boolean)

    private external fun dumpNative(path: String)

}