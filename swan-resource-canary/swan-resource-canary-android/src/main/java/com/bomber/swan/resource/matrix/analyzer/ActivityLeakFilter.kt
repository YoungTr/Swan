package com.bomber.swan.resource.matrix.analyzer

import com.bomber.swan.util.SwanLog
import shark.HeapGraph
import shark.HeapObject

/**
 * @author youngtr
 * @data 2022/4/20
 */
class ActivityLeakFilter(
    graph: HeapGraph,
    classObjectCountMap: MutableMap<Long, ObjectCounter>,
    leakingObjectIds: MutableSet<Long>,
    leakReasonTable: MutableMap<Long, String>
) :
    BaseLeakingFilter(
        graph,
        classObjectCountMap,
        leakingObjectIds,
        leakReasonTable
    ) {

    override val heapClassId: Long?
        get() = graph.findClassByName(ACTIVITY_CLASS_NAME)?.objectId

    override fun findLeaking(
        instance: HeapObject.HeapInstance,
        superId: Pair<Long, Long>
    ): Boolean {
        val superId4 = superId.second
        val result = if (superId4 == heapClassId) {
            val destroyField = instance[ACTIVITY_CLASS_NAME, DESTROYED_FIELD_NAME]!!
            val finishedField = instance[ACTIVITY_CLASS_NAME, FINISHED_FIELD_NAME]!!
            if (destroyField.value.asBoolean!! || finishedField.value.asBoolean!!) {
                val objectCounter = updateClassObjectCounterMap(instance.instanceClassId, true)
                SwanLog.i(
                    TAG, "activity name : " + instance.instanceClassName
                            + " mDestroyed:" + destroyField.value.asBoolean
                            + " mFinished:" + finishedField.value.asBoolean
                            + " objectId:" + (instance.objectId and 0xffffffffL)
                )
                if (objectCounter.leakCount <= SAME_CLASS_LEAK_OBJECT_PATH_THRESHOLD) {
                    addLeakingObjectIds(instance.objectId)
                    putLeakingObjectReason(instance.objectId, "Activity leak")
                }
            }
            true
        } else {
            false
        }

        return result
    }


    companion object {
        private const val TAG = "Swan.ActivityLeak"

        //Activity->ContextThemeWrapper->ContextWrapper->Context->Object
        private const val ACTIVITY_CLASS_NAME = "android.app.Activity"
        private const val FINISHED_FIELD_NAME = "mFinished"
        private const val DESTROYED_FIELD_NAME = "mDestroyed"
    }
}