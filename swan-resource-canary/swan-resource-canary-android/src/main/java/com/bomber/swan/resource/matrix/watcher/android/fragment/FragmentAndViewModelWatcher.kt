package com.bomber.swan.resource.matrix.watcher.android.fragment

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.bomber.swan.resource.matrix.friendly.noOpDelegate
import com.bomber.swan.resource.matrix.watcher.ReachabilityWatcher
import com.bomber.swan.resource.matrix.watcher.android.InstallableWatcher

/**
 * @author youngtr
 * @data 2022/4/10
 */
class FragmentAndViewModelWatcher(
    private val application: Application,
    private val reachabilityWatcher: ReachabilityWatcher
) : InstallableWatcher {

    private val fragmentDestroyWatchers: List<(Activity) -> Unit> = run {
        val fragmentDestroyWatchers = mutableListOf<(Activity) -> Unit>()
        // androidx fragment
        fragmentDestroyWatchers.add(AndroidXFragmentDestroyWatcher(reachabilityWatcher))
        // todo support fragment
        // todo android.app.fragment
        fragmentDestroyWatchers
    }

    private val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks by noOpDelegate() {
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
                for (watcher in fragmentDestroyWatchers) {
                    watcher(activity)
                }
            }
        }

    override fun install() {
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    override fun uninstall() {
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
    }
}