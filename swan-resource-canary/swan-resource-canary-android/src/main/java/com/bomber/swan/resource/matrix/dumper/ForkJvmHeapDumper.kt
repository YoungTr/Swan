package com.bomber.swan.resource.matrix.dumper

import android.annotation.SuppressLint
import android.os.Debug
import com.bomber.swan.util.SwanLog
import java.io.File

/**
 * @author youngtr
 * @data 2022/4/12
 */
@SuppressLint("UnsafeDynamicallyLoadedCode")
object ForkJvmHeapDumper : HeapDumper {

    private val initialized: Boolean = initialize()

    private fun initialize(): Boolean {
        System.loadLibrary("swan-resource")
        val success = initializedNative()
        SwanLog.d(TAG, "loadLibrary success: $success")
        return success == 0
    }

    override fun dumpHeap(heapDumpFile: File) {
        if (initialized) {
            when (val pid = suspendAndFork(60)) {
                0 -> Debug.dumpHprofData(heapDumpFile.absolutePath)
                -1 -> SwanLog.d(TAG, "Can't fork child dump process")
                else -> resumeAndWait(pid)
            }
        } else {
            SwanLog.d(TAG, "Fork init failed, not to dump")
        }
    }
}

private external fun initializedNative(): Int

private external fun suspendAndFork(wait: Long): Int

private external fun resumeAndWait(pid: Int): Int

private const val TAG = "Swan.ForkJvmHeapDumper"