package com.bomber.swan.resource.matrix.config

import com.bomber.swan.resource.matrix.dumper.*
import shark.*

data class ResourceConfig(

    val dumpHeapMode: DumpHeapMode = DumpHeapMode.NormalDump,

    val dumpHeapWhenDebugging: Boolean = false,

    val retainedVisibleThreshold: Int = 5,

    val referenceMatchers: List<ReferenceMatcher> = AndroidReferenceMatchers.appDefaults,

    val objectInspectors: List<ObjectInspector> = AndroidObjectInspectors.appDefaults,

    val metadataExtractor: MetadataExtractor = AndroidMetadataExtractor,

    val computeRetainedHeapSize: Boolean = true,

    val maxStoredHeapDumps: Int = 7,

    val requestWriteExternalStoragePermission: Boolean = false,

    val leakingObjectFinder: LeakingObjectFinder = KeyedWeakReferenceFinder,

    val resultCallback: IResultCallback,

    val heapDumper: HeapDumper = when (dumpHeapMode) {
        DumpHeapMode.NoDump -> NoHeapDumper
        DumpHeapMode.NormalDump -> NormalHeapDumper
        DumpHeapMode.ForkDump -> ForkJvmHeapDumper
    }
)
