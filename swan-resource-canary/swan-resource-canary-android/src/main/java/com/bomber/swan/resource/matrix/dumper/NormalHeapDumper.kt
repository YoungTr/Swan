package com.bomber.swan.resource.matrix.dumper

import android.os.Debug
import java.io.File

object NormalHeapDumper : HeapDumper {
    override fun dumpHeap(heapDumpFile: File) {
        Debug.dumpHprofData(heapDumpFile.absolutePath)
    }
}