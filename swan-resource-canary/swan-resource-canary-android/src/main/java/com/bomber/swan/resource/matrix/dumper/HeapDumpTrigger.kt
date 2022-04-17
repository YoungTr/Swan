package com.bomber.swan.resource.matrix.dumper

import android.app.Application
import android.os.Handler
import com.bomber.swan.resource.friendly.measureDurationMillis
import com.bomber.swan.resource.matrix.config.ResourceConfig
import com.bomber.swan.resource.matrix.internal.InternalSwanResource
import com.bomber.swan.resource.matrix.watcher.KeyedWeakReference
import com.bomber.swan.resource.matrix.watcher.ObjectWatcher
import com.bomber.swan.resource.matrix.watcher.android.AppWatcher
import com.bomber.swan.util.Clock
import com.bomber.swan.util.GcTrigger
import com.bomber.swan.util.SwanLog

class HeapDumpTrigger(
    private val application: Application,
    private val backgroundHandler: Handler,
    private val objectWatcher: ObjectWatcher,
    private val gcTrigger: GcTrigger,
    private val configProvider: () -> ResourceConfig,
    private val clock: Clock
) {

    @Volatile
    private var checkScheduledAt: Long = 0L

    @Volatile
    private var applicationInvisibleAt = -1L

    private var lastDisplayedRetainedObjectCount = 0

    /**
     * 上次 dump heap 的时间
     */
    private var lastHeapDumpUptimeMillis = 0L


    private val applicationVisible: Boolean
        get() = applicationInvisibleAt == -1L

    /**
     * When the app becomes invisible, we don't dump the heap immediately. Instead we wait in case
     * the app came back to the foreground, but also to wait for new leaks that typically occur on
     * back press (activity destroy).
     */
    private val applicationInvisibleLessThanWatchPeriod: Boolean
        get() {
            val applicationInvisibleAt = applicationInvisibleAt
            return applicationInvisibleAt != -1L && clock.uptimeMillis() - applicationInvisibleAt < AppWatcher.retainedDelayMillis
        }

    fun onApplicationVisibilityChanged(applicationVisible: Boolean) {
        SwanLog.d(TAG, "application visible: $applicationVisible")
        if (applicationVisible) {
            applicationInvisibleAt = -1L
        } else {
            applicationInvisibleAt = clock.uptimeMillis()
            scheduleRetainedObjectCheck(
                delayMillis = AppWatcher.retainedDelayMillis
            )
        }
    }

    /**
     * 检查是否正的存在内存泄漏
     */
    fun scheduleRetainedObjectCheck(delayMillis: Long = 0L) {
        val checkCurrentlyScheduledAt = checkScheduledAt
        if (checkCurrentlyScheduledAt > 0) {
            return
        }
        checkScheduledAt = clock.uptimeMillis() + delayMillis
        backgroundHandler.postDelayed({
            checkScheduledAt = 0
            checkRetainedObjects()
        }, delayMillis)

    }

    private fun checkRetainedObjects() {
        val config = configProvider()
        // check again
        var retainedReferenceCount = objectWatcher.retainedObjectCount
        if (retainedReferenceCount > 0) {
            gcTrigger.runGc()
            retainedReferenceCount = objectWatcher.retainedObjectCount
        }

        SwanLog.d(TAG, "retained reference count:$retainedReferenceCount")

        if (checkRetainedCount(retainedReferenceCount, config.retainedVisibleThreshold)) return

        val now = clock.uptimeMillis()
        val elapsedSinceLastDumpMillis = now - lastHeapDumpUptimeMillis
        if (elapsedSinceLastDumpMillis < WAIT_BETWEEN_HEAP_DUMPS_MILLIS) {
            // TODO:  notification if need dump
            scheduleRetainedObjectCheck(WAIT_BETWEEN_HEAP_DUMPS_MILLIS - elapsedSinceLastDumpMillis)
            return
        }

        doDumpHeap(retainedReferenceCount, true)

    }

    /**
     * 是否执行 dump 操作的判断条件
     * 1. 如果泄漏的对象数量大于等于设置的阈值，不管应用在前台还是后台都会直接 dump
     * 2. 如果 1 不成立，如果应用在前台，或者应用在后台的时间小于检测的延时时间，则会继续判断是否泄漏
     * 3. 如果应用在后台，且在后台的时间大于延时检测的时间则会去 dump heap
     * 4. 或者主动去执行 dump
     */
    private fun checkRetainedCount(retainedKeysCount: Int, retainedVisibleThreshold: Int): Boolean {
        val countChange = lastDisplayedRetainedObjectCount != retainedKeysCount
        lastDisplayedRetainedObjectCount = retainedKeysCount
        if (retainedKeysCount == 0) {
            if (countChange) {
                SwanLog.d(TAG, "All retained objects have been garbage collected")
            }
            return true
        }

        val applicationVisible = applicationVisible
        val applicationInvisibleLessThanWatchPeriod = applicationInvisibleLessThanWatchPeriod

        if (retainedKeysCount < retainedVisibleThreshold) {
            SwanLog.d(
                TAG,
                "applicationInvisibleLessThanWatchPeriod: $applicationInvisibleLessThanWatchPeriod"
            )
            if (applicationVisible || applicationInvisibleLessThanWatchPeriod) {
                // TODO:  notification if need dump
                SwanLog.d(
                    TAG,
                    "retained count($retainedKeysCount) less than retained visible threshold, wait $WAIT_FOR_OBJECT_THRESHOLD_MILLIS to recheck"
                )
                scheduleRetainedObjectCheck(delayMillis = WAIT_FOR_OBJECT_THRESHOLD_MILLIS)
                return true
            }

        }

        return false
    }

    private fun doDumpHeap(
        retainedReferenceCount: Int,
        retry: Boolean
    ) {

        SwanLog.d(TAG, "find $retainedReferenceCount retained reference, start dump heap...")
        val directoryProvider =
            InternalSwanResource.createLeakDirectoryProvider(application)
        val newHeapFile = directoryProvider.newHeapDumpFile()
        SwanLog.d(TAG, "newHeapFile: $newHeapFile")
        val durationMills: Long
        try {
            if (newHeapFile == null) {
                throw RuntimeException("Could not create heap dump file")
            }

            val heapDumpUptimeMillis = clock.uptimeMillis()
            KeyedWeakReference.heapDumpUptimeMillis = heapDumpUptimeMillis
            durationMills = measureDurationMillis {
                configProvider().heapDumper.dumpHeap(newHeapFile)
            }

            if (newHeapFile.length() == 0L) {
                throw java.lang.RuntimeException("Dumped heap file is 0 byte length")
            }
            lastDisplayedRetainedObjectCount = 0
            lastHeapDumpUptimeMillis = clock.uptimeMillis()
            objectWatcher.clearObjectsWatchedBefore(heapDumpUptimeMillis)
            SwanLog.d(TAG, "dump heap cast $durationMills ms")
            // waiting to analyse
//            InternalSwanResource.anayli
        } catch (throwable: Throwable) {
            if (retry) {
                scheduleRetainedObjectCheck(WAIT_AFTER_DUMP_FAILED_MILLIS)
            }
            return
        }

    }


    companion object {
        internal const val WAIT_AFTER_DUMP_FAILED_MILLIS = 5_000L
        private const val WAIT_FOR_OBJECT_THRESHOLD_MILLIS = 2_000L
        private const val DISMISS_NO_RETAINED_OBJECT_NOTIFICATION_MILLIS = 30_000L
        private const val WAIT_BETWEEN_HEAP_DUMPS_MILLIS = 60_000L
    }

}

private const val TAG = "Swan.HeapDumpTrigger"