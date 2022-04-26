package com.bomber.swan.resource.matrix.config

/**
 * @author youngtr
 * @data 2022/4/26
 */
fun interface IResultCallback {

    fun report(hprofFile: String, jsonFile: String, result: String)

    companion object {
        inline operator fun invoke(crossinline block: (hprofFile: String, jsonFile: String, result: String) -> Unit): IResultCallback {
            return IResultCallback { hprofFile, jsonFile, result ->
                block(
                    hprofFile,
                    jsonFile,
                    result
                )
            }
        }
    }
}