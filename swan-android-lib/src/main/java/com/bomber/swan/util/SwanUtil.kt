@file:JvmName("SwanUtil")

package com.bomber.swan.util

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader


/**
 * @author youngtr
 * @data 2022/4/9
 */
fun printFileByLine(printTAG: String?, filePath: String?) {
    var reader: BufferedReader? = null
    try {
        reader = BufferedReader(InputStreamReader(FileInputStream(File(filePath)), "UTF-8"))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            SwanLog.i(printTAG, line)
        }
    } catch (t: Throwable) {
        SwanLog.e(printTAG, "printFileByLine failed e : " + t.message)
    } finally {
        reader?.close()
    }
}