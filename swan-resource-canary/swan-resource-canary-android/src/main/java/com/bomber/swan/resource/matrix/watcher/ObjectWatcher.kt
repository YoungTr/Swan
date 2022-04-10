package com.bomber.swan.resource.matrix.watcher

import com.bomber.swan.util.Clock
import com.bomber.swan.util.SwanLog
import java.lang.ref.ReferenceQueue
import java.util.*
import java.util.concurrent.Executor

/**
 * @author youngtr
 * @data 2022/4/10
 */
class ObjectWatcher constructor(
    private val clock: Clock,
    private val checkRetainedExecutable: Executor,
    private val isEnable: () -> Boolean = { true }
) : ReachabilityWatcher {

    companion object {
        private const val TAG = "Swan.ObjectWatcher"
    }

    private val onObjectRetainedListeners = mutableSetOf<OnObjectRetainedListener>()

    private val watcherObjects = mutableMapOf<String, KeyedWeakReference>()
    private val queue = ReferenceQueue<Any>()

    /**
     * 是非存在泄漏的对象
     */
    val hasRetainedObjects: Boolean
        @Synchronized get() {
            removeWeaklyReachableObjects()
            return watcherObjects.any { it.value.retainedUptimeMillis != -1L }
        }

    /**
     * 泄漏对象的数量
     */
    val retainedObjectCount: Int
        @Synchronized get() {
            removeWeaklyReachableObjects()
            return watcherObjects.count { it.value.retainedUptimeMillis != -1L }
        }

    /**
     * 是非存在检测的对象
     */
    val hasWatchedObjects: Boolean
        @Synchronized get() {
            removeWeaklyReachableObjects()
            return watcherObjects.isNotEmpty()
        }

    /**
     * 获取所有泄漏的对象
     */
    val retainedObjects: List<Any>
        @Synchronized get() {
            removeWeaklyReachableObjects()
            val instances = mutableListOf<Any>()
            for (weakReference in watcherObjects.values) {
                if (weakReference.retainedUptimeMillis != -1L) {
                    val instance = weakReference.get()
                    if (instance != null) {
                        instances.add(instance)
                    }
                }
            }
            return instances
        }

    @Synchronized
    fun addOnObjectRetainedListener(listener: OnObjectRetainedListener) {
        onObjectRetainedListeners.add(listener)
    }

    @Synchronized
    fun removeOnObjectRetainedListener(listener: OnObjectRetainedListener) {
        onObjectRetainedListeners.remove(listener)
    }


    override fun expectWeaklyReachable(watchedObject: Any, description: String) {
        if (!isEnable()) return

        removeWeaklyReachableObjects()
        val key = UUID.randomUUID().toString()
        val watchUptimeMillis = clock.uptimeMillis()
        val reference =
            KeyedWeakReference(watchedObject, key, description, watchUptimeMillis, queue)
        SwanLog.d(
            TAG, "Watching " +
                    (if (watchedObject is Class<*>) watchedObject.toString() else "instance of ${watchedObject.javaClass.name}") +
                    (if (description.isNotEmpty()) " ($description)" else "") +
                    " with key $key"
        )

        watcherObjects[key] = reference
        checkRetainedExecutable.execute {
//            if (watcherObjects.isEmpty()) {
//
//            }
            SwanLog.d(TAG, "start execute moveToRetainer")
            moveToRetained(key)
        }

    }

    /**
     * 清除泄漏的对象
     * 注意：这些对象已经被检测过了，即 watchUptimeMillis <= heapDumpUptimeMillis
     *      未检测的对象还会保留，继续检测
     */
    @Synchronized
    fun clearObjectsWatchedBefore(heapDumpUptimeMillis: Long) {
        val weakRefsToRemove =
            watcherObjects.filter { it.value.watchUptimeMillis <= heapDumpUptimeMillis }
        weakRefsToRemove.values.forEach { it.clear() }
        watcherObjects.keys.removeAll(weakRefsToRemove.keys)
    }

    @Synchronized
    fun clearWatchedObjects() {
        watcherObjects.values.forEach { it.clear() }
        watcherObjects.clear()
    }

    @Synchronized
    private fun moveToRetained(key: String) {
        removeWeaklyReachableObjects()
        val retainedRef = watcherObjects[key]
        if (retainedRef != null) {
            retainedRef.retainedUptimeMillis = clock.uptimeMillis()
            SwanLog.d(TAG, "find retained reference ${retainedRef.get()}")
            onObjectRetainedListeners.forEach { it.onObjectRetained() }
        }
    }

    /**
     * see https://www.cnblogs.com/dreamroute/p/5029899.html
     */
    private fun removeWeaklyReachableObjects() {
        var ref: KeyedWeakReference?
        do {
            ref = queue.poll() as KeyedWeakReference?
            if (ref != null) {
                watcherObjects.remove(ref.key)
            }
        } while (ref != null)
    }
}