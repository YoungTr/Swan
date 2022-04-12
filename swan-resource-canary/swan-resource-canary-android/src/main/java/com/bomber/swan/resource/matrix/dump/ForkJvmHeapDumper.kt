package com.bomber.swan.resource.matrix.dump

import android.annotation.SuppressLint
import java.io.File

/**
 * @author youngtr
 * @data 2022/4/12
 */
@SuppressLint("UnsafeDynamicallyLoadedCode")
object ForkJvmHeapDumper : HeapDumper {



    override fun dumpHeap(heapDumpFile: File) {
        System.load("resource")
        suspendAndFork(60)
    }
}

private external fun init()

private external fun suspendAndFork(wait: Long)

private external fun resumeAndWait(pid: Int)