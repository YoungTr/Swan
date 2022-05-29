package com.bomber.swan.resource.matrix.config

import com.bomber.swan.resource.matrix.dumper.DumpHeapMode
import com.bomber.swan.resource.matrix.dumper.HeapDumper
import com.bomber.swan.resource.matrix.dumper.NoHeapDumper
import com.bomber.swan.resource.matrix.dumper.NormalHeapDumper
import shark.*

data class ResourceConfig(

    val dumpHeapMode: DumpHeapMode = DumpHeapMode.ForkDump,

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
        DumpHeapMode.ForkDump -> NormalHeapDumper
    },

    ) {
    /**
     * Construct a new Config via [ResourceConfig.Builder].
     * Note: this method is intended to be used from Java code only. For idiomatic Kotlin use
     * `copy()` to modify [ResourceConfig].
     */
    @Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
    @SinceKotlin("999.9") // Hide from Kotlin code, this method is only for Java code
    fun newBuilder() = Builder(this)

    /**
     * Builder for [ResourceConfig] intended to be used only from Java code.
     *
     * Usage:
     * ```java
     * LeakCanary.Config config = LeakCanary.getConfig().newBuilder()
     *    .retainedVisibleThreshold(3)
     *    .build();
     * LeakCanary.setConfig(config);
     * ```
     *
     * For idiomatic Kotlin use `copy()` method instead:
     * ```kotlin
     * LeakCanary.config = LeakCanary.config.copy(retainedVisibleThreshold = 3)
     * ```
     */
    class Builder internal constructor(private val config: ResourceConfig) {
        private var dumpHeapMode = config.dumpHeapMode
        private var dumpHeapWhenDebugging = config.dumpHeapWhenDebugging
        private var retainedVisibleThreshold = config.retainedVisibleThreshold

        private var referenceMatchers = config.referenceMatchers
        private var objectInspectors = config.objectInspectors

        //            private var onHeapAnalyzedListener = config.onHeapAnalyzedListener
        private var metadataExtractor = config.metadataExtractor
        private var computeRetainedHeapSize = config.computeRetainedHeapSize
        private var maxStoredHeapDumps = config.maxStoredHeapDumps
        private var requestWriteExternalStoragePermission =
            config.requestWriteExternalStoragePermission

        private var leakingObjectFinder = config.leakingObjectFinder
        private var heapDumper = config.heapDumper
//            private var eventListeners = config.eventListeners

        fun dumpHeap(dumpHeap: DumpHeapMode) =
            apply { this.dumpHeapMode = dumpHeap }

        fun dumpHeapWhenDebugging(dumpHeapWhenDebugging: Boolean) =
            apply { this.dumpHeapWhenDebugging = dumpHeapWhenDebugging }

        fun retainedVisibleThreshold(retainedVisibleThreshold: Int) =
            apply { this.retainedVisibleThreshold = retainedVisibleThreshold }

        fun referenceMatchers(referenceMatchers: List<ReferenceMatcher>) =
            apply { this.referenceMatchers = referenceMatchers }

        fun objectInspectors(objectInspectors: List<ObjectInspector>) =
            apply { this.objectInspectors = objectInspectors }

        fun metadataExtractor(metadataExtractor: MetadataExtractor) =
            apply { this.metadataExtractor = metadataExtractor }

        fun computeRetainedHeapSize(computeRetainedHeapSize: Boolean) =
            apply { this.computeRetainedHeapSize = computeRetainedHeapSize }

        fun maxStoredHeapDumps(maxStoredHeapDumps: Int) =
            apply { this.maxStoredHeapDumps = maxStoredHeapDumps }

        fun requestWriteExternalStoragePermission(requestWriteExternalStoragePermission: Boolean) =
            apply {
                this.requestWriteExternalStoragePermission =
                    requestWriteExternalStoragePermission
            }

        fun leakingObjectFinder(leakingObjectFinder: LeakingObjectFinder) =
            apply { this.leakingObjectFinder = leakingObjectFinder }

        fun heapDumper(heapDumper: HeapDumper) =
            apply { this.heapDumper = heapDumper }

//            fun eventListeners(eventListeners: List<EventListener>) =
//                apply { this.eventListeners = eventListeners }


        fun build() = config.copy(
            dumpHeapMode = dumpHeapMode,
            dumpHeapWhenDebugging = dumpHeapWhenDebugging,
            retainedVisibleThreshold = retainedVisibleThreshold,
            referenceMatchers = referenceMatchers,
            objectInspectors = objectInspectors,
//                onHeapAnalyzedListener = onHeapAnalyzedListener,
            metadataExtractor = metadataExtractor,
            computeRetainedHeapSize = computeRetainedHeapSize,
            maxStoredHeapDumps = maxStoredHeapDumps,
            requestWriteExternalStoragePermission = requestWriteExternalStoragePermission,
            leakingObjectFinder = leakingObjectFinder,
            heapDumper = heapDumper,
//                eventListeners = eventListeners,
        )
    }
}
