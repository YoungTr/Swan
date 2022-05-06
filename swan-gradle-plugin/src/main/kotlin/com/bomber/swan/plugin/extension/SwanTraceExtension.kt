package com.bomber.swan.plugin.extension

class SwanTraceExtension(
    var baseMethodMapFile: String,
    var blackListFile: String,
    var customDexTransformName: String,
    var skipCheckClass: Boolean = true
) {

    override fun toString(): String {
        return "SwanTraceExtension(baseMethodMapFile='$baseMethodMapFile', blackListFile='$blackListFile', customDexTransformName='$customDexTransformName', skipCheckClass=$skipCheckClass)"
    }
}