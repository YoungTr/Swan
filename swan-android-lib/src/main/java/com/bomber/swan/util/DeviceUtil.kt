package com.bomber.swan.util

import android.os.Build

/**
 * @author youngtr
 * @data 2022/5/23
 */

fun is64BitRuntime(): Boolean {
    val currRuntimeABIs = Build.SUPPORTED_ABIS
    for (api in currRuntimeABIs) {
        if ("arm64-v8a".equals(api, true)
            || "x86_64".equals(api, true)
            || "mips64".equals(api, true)
        ) {
            return true
        }
    }
    return false
}