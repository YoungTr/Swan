package com.bomber.swan.resource.matrix.watcher.android.fragment

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.bomber.swan.resource.matrix.watcher.ReachabilityWatcher

/**
 * @author youngtr
 * @data 2022/4/10
 */
internal class AndroidXFragmentDestroyWatcher(
    private val reachabilityWatcher: ReachabilityWatcher
) : (Activity) -> Unit {

    private val fragmentLifecycleCallback = object : FragmentManager.FragmentLifecycleCallbacks() {

        override fun onFragmentCreated(
            fragmentManager: FragmentManager,
            fragment: Fragment,
            savedInstanceState: Bundle?
        ) {
            ViewModelClearedWatcher.install(fragment, reachabilityWatcher)
        }

        override fun onFragmentViewDestroyed(fragmentManager: FragmentManager, fragment: Fragment) {
            val view = fragment.view
            view?.let {
                reachabilityWatcher.expectWeaklyReachable(
                    it, "${fragment::class.java.name} received Fragment#onDestroyView() callback " +
                            "(references to its views should be cleared to prevent leaks)"
                )
            }

        }

        override fun onFragmentDetached(fragmentManager: FragmentManager, fragment: Fragment) {
            reachabilityWatcher.expectWeaklyReachable(
                fragment,
                "${fragment::class.java.name} received Fragment#onDestroy() callback"
            )
        }
    }

    override fun invoke(activity: Activity) {
        if (activity is FragmentActivity) {
            val supportFragmentManager = activity.supportFragmentManager
            supportFragmentManager.registerFragmentLifecycleCallbacks(
                fragmentLifecycleCallback,
                true
            )
            ViewModelClearedWatcher.install(activity, reachabilityWatcher)
        }

    }
}