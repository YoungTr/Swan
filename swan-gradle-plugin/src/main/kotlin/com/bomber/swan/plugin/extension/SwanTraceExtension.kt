package com.bomber.swan.plugin.extension

open class SwanTraceExtension(
    var baseMethodMapFile: String = "",
    var blackListFile: String = "",
    var customDexTransformName: String = "",
    var transformInjectionForced: Boolean = false,
    var skipCheckClass: Boolean = true,
    var enable: Boolean = false
) {

    override fun toString(): String {
        return "SwanTraceExtension(baseMethodMapFile='$baseMethodMapFile', blackListFile='$blackListFile', customDexTransformName='$customDexTransformName', skipCheckClass=$skipCheckClass)"
    }
}