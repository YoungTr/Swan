package com.bomber.swan.resource.matrix.analyzer

import shark.HeapGraph
import shark.HeapObject

/**
 * @author youngtr
 * @data 2022/4/20
 */
abstract class BaseLeakingFilter(
    val graph: HeapGraph,
    private val classObjectCountMap: MutableMap<Long, ObjectCounter>,
    private val leakingObjectIds: MutableSet<Long>,
    private val leakReasonTable: MutableMap<Long, String>
) {

    abstract val heapClassId: Long?


    internal fun updateClassObjectCounterMap(
        instanceClassId: Long,
        isLeak: Boolean
    ): ObjectCounter {
        val objectCounter = classObjectCountMap[instanceClassId]
            ?: ObjectCounter().also { classObjectCountMap[instanceClassId] }

        objectCounter.allCounter++
        if (isLeak) {
            objectCounter.leakCount++
        }
        return objectCounter
    }

    internal fun addLeakingObjectIds(objectId: Long) {
        leakingObjectIds.add(objectId)
    }

    internal fun putLeakingObjectReason(objectId: Long, reason: String) {
        leakReasonTable[objectId] = reason
    }

    abstract fun findLeaking(instance: HeapObject.HeapInstance, superId: Pair<Long, Long>): Boolean

    companion object {
        internal const val SAME_CLASS_LEAK_OBJECT_PATH_THRESHOLD = 45

    }
}