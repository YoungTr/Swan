package com.bomber.swan.resource.matrix.dump

import android.app.Application
import android.os.Handler
import com.bomber.swan.resource.matrix.config.ResourceConfig
import com.bomber.swan.resource.matrix.watcher.ObjectWatcher
import com.bomber.swan.util.GcTrigger

class HeapDumpTrigger(
    private val application: Application,
    private val backgroundHandler: Handler,
    private val objectWatcher: ObjectWatcher,
    private val gcTrigger: GcTrigger,
    private val config: () -> ResourceConfig
) {

    @Volatile
    private var checkScheduledAt: Long = 0L

    fun onApplicationVisibilityChanged(applicationVisible: Boolean) {

    }

    /**
     * 开始检查是否正的存在内存泄漏
     */
    fun scheduleRetainedObjectCheck(delayMillis: Long = 0L) {
        val checkCurrentlyScheduledAt = checkScheduledAt
        if (checkCurrentlyScheduledAt > 0) {
            return
        }
        checkScheduledAt = System.currentTimeMillis() + delayMillis
        backgroundHandler.postDelayed({
            checkScheduledAt = 0
            checkRetainedObjects()
        }, delayMillis)

    }

    private fun checkRetainedObjects() {

    }


}