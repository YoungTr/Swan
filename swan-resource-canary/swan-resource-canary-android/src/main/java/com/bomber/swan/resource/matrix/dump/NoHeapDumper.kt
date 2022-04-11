package com.bomber.swan.resource.matrix.dump

import com.bomber.swan.resource.matrix.watcher.android.AppWatcher
import com.bomber.swan.util.SwanLog
import java.io.File

object NoHeapDumper : HeapDumper {
    override fun dumpHeap(heapDumpFile: File) {
        val retainedObjects = AppWatcher.objectWatcher.retainedObjects
        SwanLog.d(TAG, "retain objects: $retainedObjects")
    }
}

private const val TAG = "Swan.NoHeapDumper"