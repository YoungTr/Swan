package com.bomber.swan.resource.matrix.watcher

/**
 * @author youngtr
 * @data 2022/4/10
 */
interface ReachabilityWatcher {

    fun expectWeaklyReachable(watchedObject: Any, description: String)
}