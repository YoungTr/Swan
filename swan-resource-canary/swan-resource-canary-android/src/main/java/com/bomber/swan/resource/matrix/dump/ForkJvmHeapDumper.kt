package com.bomber.swan.resource.matrix.dump

import android.annotation.SuppressLint
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
            suspendAndFork(60)
            SwanLog.d(TAG, "after suspend")
        }
    }
}

private external fun initializedNative(): Int

private external fun suspendAndFork(wait: Long)

private external fun resumeAndWait(pid: Int)

private const val TAG = "Swan.ForkJvmHeapDumper"