package com.bomber.swan.resource.matrix.config

import com.bomber.swan.resource.matrix.ResourceMatrixPlugin
import com.bomber.swan.resource.matrix.dumper.DumpHeapMode
import com.bomber.swan.resource.matrix.dumper.HeapDumper
import com.bomber.swan.resource.matrix.dumper.NoHeapDumper
import com.bomber.swan.resource.matrix.dumper.NormalHeapDumper
import shark.*

data class ResourceConfig(
    /**
     * Whether LeakCanary should dump the heap when enough retained instances are found. This needs
     * to be true for LeakCanary to work, but sometimes you may want to temporarily disable
     * LeakCanary (e.g. for a product demo).
     *
     * Defaults to true.
     */
    val dumpHeapMode: DumpHeapMode = DumpHeapMode.ForkDump,
    /**
     * If [dumpHeapWhenDebugging] is false then LeakCanary will not dump the heap
     * when the debugger is attached. The debugger can create temporary memory leaks (for instance
     * if a thread is blocked on a breakpoint).
     *
     * Defaults to false.
     */
    val dumpHeapWhenDebugging: Boolean = false,
    /**
     * When the app is visible, LeakCanary will wait for at least
     * [retainedVisibleThreshold] retained instances before dumping the heap. Dumping the heap
     * freezes the UI and can be frustrating for developers who are trying to work. This is
     * especially frustrating as the Android Framework has a number of leaks that cannot easily
     * be fixed.
     *
     * When the app becomes invisible, LeakCanary dumps the heap after
     * [AppWatcher.retainedDelayMillis] ms.
     *
     * The app is considered visible if it has at least one activity in started state.
     *
     * A higher threshold means LeakCanary will dump the heap less often, therefore it won't be
     * bothering developers as much but it could miss some leaks.
     *
     * Defaults to 5.
     */
    val retainedVisibleThreshold: Int = 5,

    /**
     * Known patterns of references in the heap, added here either to ignore them
     * ([IgnoredReferenceMatcher]) or to mark them as library leaks ([LibraryLeakReferenceMatcher]).
     *
     * When adding your own custom [LibraryLeakReferenceMatcher] instances, you'll most
     * likely want to set [LibraryLeakReferenceMatcher.patternApplies] with a filter that checks
     * for the Android OS version and manufacturer. The build information can be obtained by calling
     * [shark.AndroidBuildMirror.fromHeapGraph].
     *
     * Defaults to [AndroidReferenceMatchers.appDefaults]
     */
    val referenceMatchers: List<ReferenceMatcher> = AndroidReferenceMatchers.appDefaults,

    /**
     * List of [ObjectInspector] that provide LeakCanary with insights about objects found in the
     * heap. You can create your own [ObjectInspector] implementations, and also add
     * a [shark.AppSingletonInspector] instance created with the list of internal singletons.
     *
     * Defaults to [AndroidObjectInspectors.appDefaults]
     */
    val objectInspectors: List<ObjectInspector> = AndroidObjectInspectors.appDefaults,


    /**
     * Extracts metadata from a hprof to be reported in [HeapAnalysisSuccess.metadata].
     * Called on a background thread during heap analysis.
     *
     * Defaults to [AndroidMetadataExtractor]
     */
    val metadataExtractor: MetadataExtractor = AndroidMetadataExtractor,

    /**
     * Whether to compute the retained heap size, which is the total number of bytes in memory that
     * would be reclaimed if the detected leaks didn't happen. This includes native memory
     * associated to Java objects (e.g. Android bitmaps).
     *
     * Computing the retained heap size can slow down the analysis because it requires navigating
     * from GC roots through the entire object graph, whereas [shark.HeapAnalyzer] would otherwise
     * stop as soon as all leaking instances are found.
     *
     * Defaults to true.
     */
    val computeRetainedHeapSize: Boolean = true,

    /**
     * How many heap dumps are kept on the Android device for this app package. When this threshold
     * is reached LeakCanary deletes the older heap dumps. As several heap dumps may be enqueued
     * you should avoid going down to 1 or 2.
     *
     * Defaults to 7.
     */
    val maxStoredHeapDumps: Int = 7,

    /**
     * LeakCanary always attempts to store heap dumps on the external storage if the
     * WRITE_EXTERNAL_STORAGE is already granted, and otherwise uses the app storage.
     * If the WRITE_EXTERNAL_STORAGE permission is not granted and
     * [requestWriteExternalStoragePermission] is true, then LeakCanary will display a notification
     * to ask for that permission.
     *
     * Defaults to false because that permission notification can be annoying.
     */
    val requestWriteExternalStoragePermission: Boolean = false,

    /**
     * Finds the objects that are leaking, for which LeakCanary will compute leak traces.
     *
     * Defaults to [KeyedWeakReferenceFinder] which finds all objects tracked by a
     * [KeyedWeakReference], ie all objects that were passed to
     * [ObjectWatcher.expectWeaklyReachable].
     *
     * You could instead replace it with a [FilteringLeakingObjectFinder], which scans all objects
     * in the heap dump and delegates the decision to a list of
     * [FilteringLeakingObjectFinder.LeakingObjectFilter]. This can lead to finding more leaks
     * than the default and shorter leak traces. This also means that every analysis during a
     * given process life will bring up the same leaking objects over and over again, unlike
     * when using [KeyedWeakReferenceFinder] (because [KeyedWeakReference] instances are cleared
     * after each heap dump).
     *
     * The list of filters can be built from [AndroidObjectInspectors]:
     *
     * ```kotlin
     * LeakCanary.config = LeakCanary.config.copy(
     *     leakingObjectFinder = FilteringLeakingObjectFinder(
     *         AndroidObjectInspectors.appLeakingObjectFilters
     *     )
     * )
     * ```
     */
    val leakingObjectFinder: LeakingObjectFinder = KeyedWeakReferenceFinder,

    /**
     * Dumps the Java heap. You may replace this with your own implementation if you wish to
     * change the core heap dumping implementation.
     */
    val heapDumper: HeapDumper = when (dumpHeapMode) {
        DumpHeapMode.NoDump -> NoHeapDumper
        DumpHeapMode.NormalDump -> NormalHeapDumper
        DumpHeapMode.ForkDump -> NormalHeapDumper
    },

    /**
     * Listeners for LeakCanary events. See [EventListener.Event] for the list of events and
     * which thread they're sent from. You most likely want to keep this list and add to it, or
     * remove a few entries but not all entries. Each listener is independent and provides
     * additional behavior which you can disable by not excluding it:
     *
     * ```kotlin
     * // No cute canary toast (very sad!)
     * LeakCanary.config = LeakCanary.config.run {
     *   copy(
     *     eventListeners = eventListeners.filter {
     *       it !is ToastEventListener
     *     }
     *   )
     * }
     * ```
     */
//        val eventListeners: List<EventListener> = listOf(
//            LogcatEventListener,
//            ToastEventListener,
//            LazyForwardingEventListener {
//                if (InternalLeakCanary.formFactor == TV) TvEventListener else NotificationEventListener
//            },
//            when {
//                RemoteWorkManagerHeapAnalyzer.remoteLeakCanaryServiceInClasspath ->
//                    RemoteWorkManagerHeapAnalyzer
//                WorkManagerHeapAnalyzer.workManagerInClasspath -> WorkManagerHeapAnalyzer
//                else -> BackgroundThreadHeapAnalyzer
//            }
) {
    /**
     * Construct a new Config via [SwanResource.Config.Builder].
     * Note: this method is intended to be used from Java code only. For idiomatic Kotlin use
     * `copy()` to modify [SwanResource.config].
     */
    @Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
    @SinceKotlin("999.9") // Hide from Kotlin code, this method is only for Java code
    fun newBuilder() = Builder(this)

    /**
     * Builder for [SwanResource.Config] intended to be used only from Java code.
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
    class Builder internal constructor(config: ResourceConfig) {
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

        /** @see [SwanResource.Config.dumpHeap] */
        fun dumpHeap(dumpHeap: DumpHeapMode) =
            apply { this.dumpHeapMode = dumpHeap }

        /** @see [SwanResource.Config.dumpHeapWhenDebugging] */
        fun dumpHeapWhenDebugging(dumpHeapWhenDebugging: Boolean) =
            apply { this.dumpHeapWhenDebugging = dumpHeapWhenDebugging }

        /** @see [SwanResource.Config.retainedVisibleThreshold] */
        fun retainedVisibleThreshold(retainedVisibleThreshold: Int) =
            apply { this.retainedVisibleThreshold = retainedVisibleThreshold }

        /** @see [SwanResource.Config.referenceMatchers] */
        fun referenceMatchers(referenceMatchers: List<ReferenceMatcher>) =
            apply { this.referenceMatchers = referenceMatchers }

        /** @see [SwanResource.Config.objectInspectors] */
        fun objectInspectors(objectInspectors: List<ObjectInspector>) =
            apply { this.objectInspectors = objectInspectors }


        //            /** @see [SwanResource.Config.metadataExtractor] */
        fun metadataExtractor(metadataExtractor: MetadataExtractor) =
            apply { this.metadataExtractor = metadataExtractor }

        /** @see [SwanResource.Config.computeRetainedHeapSize] */
        fun computeRetainedHeapSize(computeRetainedHeapSize: Boolean) =
            apply { this.computeRetainedHeapSize = computeRetainedHeapSize }

        /** @see [SwanResource.Config.maxStoredHeapDumps] */
        fun maxStoredHeapDumps(maxStoredHeapDumps: Int) =
            apply { this.maxStoredHeapDumps = maxStoredHeapDumps }

        /** @see [SwanResource.Config.requestWriteExternalStoragePermission] */
        fun requestWriteExternalStoragePermission(requestWriteExternalStoragePermission: Boolean) =
            apply {
                this.requestWriteExternalStoragePermission =
                    requestWriteExternalStoragePermission
            }

        /** @see [SwanResource.Config.leakingObjectFinder] */
        fun leakingObjectFinder(leakingObjectFinder: LeakingObjectFinder) =
            apply { this.leakingObjectFinder = leakingObjectFinder }

        /** @see [SwanResource.Config.heapDumper] */
//            fun heapDumper(heapDumper: HeapDumper) =
//                apply { this.heapDumper = heapDumper }

        /** @see [SwanResource.Config.eventListeners] */
//            fun eventListeners(eventListeners: List<EventListener>) =
//                apply { this.eventListeners = eventListeners }


        fun build() = ResourceMatrixPlugin.config.copy(
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
