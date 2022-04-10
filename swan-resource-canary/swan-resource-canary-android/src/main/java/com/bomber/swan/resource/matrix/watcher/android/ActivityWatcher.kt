package com.bomber.swan.resource.matrix.watcher.android

import android.app.Activity
import android.app.Application
import com.bomber.swan.resource.matrix.friendly.noOpDelegate
import com.bomber.swan.resource.matrix.watcher.ReachabilityWatcher

/**
 * @author youngtr
 * @data 2022/4/10
 */
class ActivityWatcher(
    private val application: Application,
    private val reachabilityWatcher: ReachabilityWatcher
) : InstallableWatcher {

    private val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks by noOpDelegate() {
            override fun onActivityDestroyed(activity: Activity) {
                reachabilityWatcher.expectWeaklyReachable(
                    activity,
                    "${activity::class.java.name} received Activity#onDestroy() callback)"
                )
            }
        }

    override fun install() {
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    override fun uninstall() {
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
    }
}