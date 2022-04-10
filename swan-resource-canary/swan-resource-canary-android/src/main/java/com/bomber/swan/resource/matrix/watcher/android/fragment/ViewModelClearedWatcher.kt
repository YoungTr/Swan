package com.bomber.swan.resource.matrix.watcher.android.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.bomber.swan.resource.matrix.watcher.ReachabilityWatcher

/**
 * @author youngtr
 * @data 2022/4/10
 */
internal class ViewModelClearedWatcher(
    storeOwner: ViewModelStoreOwner,
    private val reachabilityWatcher: ReachabilityWatcher

) : ViewModel() {

    private val viewModelMap: Map<String, ViewModel>? = try {
        val mMapField =
            ViewModelStore::class.java.getDeclaredField("mMap").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        mMapField[storeOwner.viewModelStore] as Map<String, ViewModel>
    } catch (ignore: Exception) {
        null
    }

    override fun onCleared() {
        viewModelMap?.values?.forEach { viewModel ->
            reachabilityWatcher.expectWeaklyReachable(
                viewModel,
                "${viewModel::class.java.name} received ViewModel#onCleared() callback"
            )
        }
    }

    companion object {
        fun install(
            storeOwner: ViewModelStoreOwner,
            reachabilityWatcher: ReachabilityWatcher
        ) {

            val provider = ViewModelProvider(storeOwner, object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return ViewModelClearedWatcher(storeOwner, reachabilityWatcher) as T
                }
            })
            provider.get(ViewModelClearedWatcher::class.java)
        }
    }
}