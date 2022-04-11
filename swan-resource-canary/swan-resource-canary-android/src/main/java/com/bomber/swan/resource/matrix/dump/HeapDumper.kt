package com.bomber.swan.resource.matrix.dump

import java.io.File

fun interface HeapDumper {
    fun dumpHeap(heapDumpFile: File)
}