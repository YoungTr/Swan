package com.bomber.swan.resource.matrix.dumper

import java.io.File

fun interface HeapDumper {
    fun dumpHeap(heapDumpFile: File)
}