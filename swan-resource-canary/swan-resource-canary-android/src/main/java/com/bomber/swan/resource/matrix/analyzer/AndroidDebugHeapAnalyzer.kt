package com.bomber.swan.resource.matrix.analyzer

import android.content.Intent
import android.os.Build
import android.os.Debug
import com.bomber.swan.resource.friendly.toMB
import com.bomber.swan.resource.matrix.EventListener.Event.*
import com.bomber.swan.resource.matrix.EventListener.Event.HeapAnalysisDone.HeapAnalysisSucceeded
import com.bomber.swan.resource.matrix.internal.InternalSwanResource
import com.bomber.swan.resource.matrix.internal.LeakDirectoryProvider
import com.bomber.swan.util.SwanLog
import com.bomber.swan.util.SystemInfo.javaHeap
import com.bomber.swan.util.SystemInfo.memInfo
import com.bomber.swan.util.SystemInfo.procStatus
import com.bomber.swan.util.versionName
import com.google.gson.Gson
import shark.*
import shark.HprofHeapGraph.Companion.openHeapGraph
import java.io.File

/**
 * @author youngtr
 * @data 2022/4/17
 */
object AndroidDebugHeapAnalyzer {

    private const val TAG = "Swan.HeapAnalyzer"

    //Activity->ContextThemeWrapper->ContextWrapper->Context->Object
    private const val ACTIVITY_CLASS_NAME = "android.app.Activity"

    //Bitmap->Object
    //Exception: Some OPPO devices
    private const val BITMAP_CLASS_NAME = "android.graphics.Bitmap"

    // pre-androidx, support library version of the Fragment implementation.
    private const val ANDROIDX_FRAGMENT_CLASS_NAME = "androidx.fragment.app.Fragment"

    // androidx version of the Fragment implementation
    //Window->Object
    private const val WINDOW_CLASS_NAME = "android.view.Window"

    //NativeAllocationRegistry
    private const val NATIVE_ALLOCATION_CLASS_NAME = "libcore.util.NativeAllocationRegistry"
    private const val NATIVE_ALLOCATION_CLEANER_THUNK_CLASS_NAME =
        "libcore.util.NativeAllocationRegistry\$CleanerThunk"

    private const val FINISHED_FIELD_NAME = "mFinished"
    private const val DESTROYED_FIELD_NAME = "mDestroyed"

    private const val FRAGMENT_MANAGER_FIELD_NAME = "mFragmentManager"
    private const val FRAGMENT_MCALLED_FIELD_NAME = "mCalled"

    private const val DEFAULT_BIG_PRIMITIVE_ARRAY = 256 * 1024
    private const val DEFAULT_BIG_BITMAP = 768 * 1366 + 1
    private const val DEFAULT_BIG_OBJECT_ARRAY = 256 * 1024
    private const val SAME_CLASS_LEAK_OBJECT_PATH_THRESHOLD = 45


    fun runAnalysisBlocking(
        heapDumped: HeapDump,
        isCanceled: () -> Boolean = { false },
        processEventListener: (HeapAnalysisProgress) -> Unit
    ): HeapAnalysisDone<*> {
        val processListener = OnAnalysisProgressListener { step ->
            val percent = (step.ordinal * 1.0) / OnAnalysisProgressListener.Step.values().size
            processEventListener(HeapAnalysisProgress(heapDumped.uniqueId, step, percent))
        }

        val heapDumpFile = heapDumped.file
        val heapDumpDurationMillis = heapDumped.durationMillis
        val heapDumpReason = heapDumped.reason

        SwanLog.d(TAG, "runAnalysisBlocking")


        val heapAnalysis = if (heapDumpFile.exists()) {
            analyzeHeap(heapDumpFile, processListener, isCanceled)
        } else {
            missingFileFailure(heapDumpFile)
        }

        val fullHeapAnalysis = when (heapAnalysis) {
            is HeapAnalysisSuccess -> heapAnalysis.copy(
                dumpDurationMillis = heapDumpDurationMillis,
                metadata = heapAnalysis.metadata + ("Heap dump reason" to heapDumpReason)
            )
            is HeapAnalysisFailure -> {
                val failureCause = heapAnalysis.exception.cause!!
                if (failureCause is OutOfMemoryError) {
                    heapAnalysis.copy(
                        dumpDurationMillis = heapDumpDurationMillis,
                        exception = HeapAnalysisException(
                            RuntimeException(
                                """
              Not enough memory to analyze heap. You can:
              - Kill the app then restart the analysis from the LeakCanary activity.
              - Increase the memory available to your debug app with largeHeap=true: https://developer.android.com/guide/topics/manifest/application-element#largeHeap
              - Set up LeakCanary to run in a separate process: https://square.github.io/leakcanary/recipes/#running-the-leakcanary-analysis-in-a-separate-process
              - Download the heap dump from the LeakCanary activity then run the analysis from your computer with shark-cli: https://square.github.io/leakcanary/shark/#shark-cli
            """.trimIndent(), failureCause
                            )
                        )
                    )
                } else {
                    heapAnalysis.copy(dumpDurationMillis = heapDumpDurationMillis)
                }
            }
        }

        // TODO: process dump

//        val analysisDoneEvent = when (fullHeapAnalysis) {
//            is HeapAnalysisSuccess -> {
//                val showIntent = LeakActivity.createSuccessIntent(application, id)
//                val leakSignatures = fullHeapAnalysis.allLeaks.map { it.signature }.toSet()
//                val leakSignatureStatuses = LeakTable.retrieveLeakReadStatuses(db, leakSignatures)
//                val unreadLeakSignatures = leakSignatureStatuses.filter { (_, read) ->
//                    !read
//                }.keys
//                    // keys returns LinkedHashMap$LinkedKeySet which isn't Serializable
//                    .toSet()
//                EventListener.Event.HeapAnalysisDone.HeapAnalysisSucceeded(
//                    heapDumped.uniqueId,
//                    fullHeapAnalysis,
//                    unreadLeakSignatures,
//                    showIntent
//                )
//            }
//            is HeapAnalysisFailure -> {
//                val showIntent = LeakActivity.createFailureIntent(application, id)
//                EventListener.Event.HeapAnalysisDone.HeapAnalysisFailed(
//                    heapDumped.uniqueId,
//                    fullHeapAnalysis,
//                    showIntent
//                )
//            }
//        }

        return HeapAnalysisSucceeded(
            heapDumped.uniqueId,
            fullHeapAnalysis as HeapAnalysisSuccess,
            setOf(),
            Intent()
        )
    }

    private fun analyzeHeap(
        heapDumpFile: File,
        processEventListener: OnAnalysisProgressListener,
        canceled: () -> Boolean
    ): HeapAnalysis {

        val startTime = System.currentTimeMillis()

        val graph =
            heapDumpFile.openHeapGraph(
                indexedGcRootTypes = setOf(
                    HprofRecordTag.ROOT_JNI_GLOBAL,
                    HprofRecordTag.ROOT_JNI_LOCAL,
                    HprofRecordTag.ROOT_NATIVE_STACK,
                    HprofRecordTag.ROOT_STICKY_CLASS,
                    HprofRecordTag.ROOT_THREAD_BLOCK,
                    HprofRecordTag.ROOT_THREAD_OBJECT,
                )
            )

        //缓存classHierarchy，用于查找class的所有instance
        val classHierarchyMap = mutableMapOf<Long, Pair<Long, Long>>()

        // 记录class objects数量
        val classObjectCounterMap = mutableMapOf<Long, ObjectCounter>()
        //
        val leakingObjectIds = mutableSetOf<Long>()
        val leakReasonTable = mutableMapOf<Long, String>()

        val runningInfo = RunningInfo(
            jvmMax = javaHeap.max.toMB().toString(),
            jvmUsed = (javaHeap.total - javaHeap.free).toMB().toString(),
            vss = procStatus.vssInKb.toMB().toString() + "mb",
            pss = Debug.getPss().toMB().toString() + "mb",
            // thread count
            fdCount = (File("/proc/self/fd").listFiles()?.size ?: 0).toString(),
            // thread list
            // fd list
            sdkInt = Build.VERSION.SDK_INT.toString(),
            manufacture = Build.MANUFACTURER.toString(),
            buildModel = Build.MODEL.toString(),
            appVersion = InternalSwanResource.application.versionName(),
            currentPage = "",
            usageSeconds = "1",
            nowTime = System.currentTimeMillis().toString(),
            deviceMemTotal = memInfo.totalInKb.toMB().toString(),
            deviceMemAvailable = memInfo.availableInKb.toMB().toString(),
            dumpReason = "find leak",
            analysisReason = ""
        )
        val heapReport = HeapReport(runningInfo)

        val activityHeapClass = graph.findClassByName(ACTIVITY_CLASS_NAME)
        val fragmentHeapClass = graph.findClassByName(ANDROIDX_FRAGMENT_CLASS_NAME)
        val bitmapHeapCass = graph.findClassByName(BITMAP_CLASS_NAME)
        val nativeAllocationHeapClass = graph.findClassByName(NATIVE_ALLOCATION_CLASS_NAME)
        val nativeAllocationThunkHeapClass = graph.findClassByName(
            NATIVE_ALLOCATION_CLEANER_THUNK_CLASS_NAME
        )
        val windowClass = graph.findClassByName(WINDOW_CLASS_NAME)

        for (instance in graph.instances) {
            if (instance.isPrimitiveWrapper) continue

            val instanceClassId = instance.instanceClassId

            /**
             * 使用HashMap缓存及遍历两边classHierarchy，这2种方式加速查找instance是否是对应类实例
             * superId1代表类的继承层次中倒数第一的id，0就是继承自object
             * superId4代表类的继承层次中倒数第四的id
             * 类的继承关系，以AOSP代码为主，部分厂商入如OPPO Bitmap会做一些修改，这里先忽略
             */
            val (superId1, superId4) = if (classHierarchyMap[instanceClassId] != null) {
                classHierarchyMap[instanceClassId]!!
            } else {
                val classHierarchyList = instance.instanceClass.classHierarchy.toList()
                val first =
                    classHierarchyList.getOrNull(classHierarchyList.size - 2)?.objectId ?: -1L
                val second =
                    classHierarchyList.getOrNull(classHierarchyList.size - 5)?.objectId ?: -1L
                Pair(first, second).also { classHierarchyMap[instanceClassId] }
            }

            /**
             * 遍历镜像所有class查找
             *
             * 计算gc path：
             * 1.已经destroyed和finished的activity
             * 2.已经fragment manager为空的fragment
             * 3.已经destroyed的window
             * 4.超过阈值大小的bitmap
             * 5.超过阈值大小的基本类型数组
             * 6.超过阈值大小的对象个数的任意class
             *
             *
             * 记录关键类:
             * 对象数量
             * 1.基本类型数组
             * 2.Bitmap
             * 3.NativeAllocationRegistry
             * 4.超过阈值大小的对象的任意class
             *
             *
             * 记录大对象:
             * 对象大小
             * 1.Bitmap
             * 2.基本类型数组
             */
            // Activity
            if (activityHeapClass?.objectId == superId4) {
                val destroyField = instance[ACTIVITY_CLASS_NAME, DESTROYED_FIELD_NAME]!!
                val finishedField = instance[ACTIVITY_CLASS_NAME, FINISHED_FIELD_NAME]!!
                if (destroyField.value.asBoolean!! || finishedField.value.asBoolean!!) {
                    val objectCounter =
                        updateClassObjectCounterMap(classObjectCounterMap, instanceClassId, true)
                    SwanLog.i(
                        TAG, "activity name : " + instance.instanceClassName
                                + " mDestroyed:" + destroyField.value.asBoolean
                                + " mFinished:" + finishedField.value.asBoolean
                                + " objectId:" + (instance.objectId and 0xffffffffL)
                    )
                    if (objectCounter.leakCount <= SAME_CLASS_LEAK_OBJECT_PATH_THRESHOLD) {
                        leakingObjectIds.add(instance.objectId)
                        leakReasonTable[instance.objectId] = "Activity Leak"
                    }

                }
                continue
            }

            // Fragment
            if (fragmentHeapClass?.objectId == superId1) {
                val className = fragmentHeapClass.name
                SwanLog.d(TAG, "fragment class name: $className")
                val fragmentManager = instance[className, FRAGMENT_MANAGER_FIELD_NAME]
                if (fragmentManager != null && fragmentManager.value.asObject == null) {
                    val mCalledField = instance[className, FRAGMENT_MCALLED_FIELD_NAME]
                    val isLeak = mCalledField != null && mCalledField.value.asBoolean!!
                    val objectCounter =
                        updateClassObjectCounterMap(classObjectCounterMap, instanceClassId, isLeak)
                    SwanLog.d(TAG, "fragment name: ${instance.instanceClassName} isLeak $isLeak")
                    if (objectCounter.leakCount <= SAME_CLASS_LEAK_OBJECT_PATH_THRESHOLD && isLeak) {
                        leakingObjectIds.add(instance.objectId)
                        leakReasonTable[instance.objectId] = "Fragment leak"
                    }
                }

                continue
            }

            // bitmap
            if (bitmapHeapCass?.objectId == superId1) {
                val fieldWidth = instance[BITMAP_CLASS_NAME, "mWidth"]
                val fieldHeight = instance[BITMAP_CLASS_NAME, "mHeight"]

                val with = fieldWidth!!.value.asInt!!
                val height = fieldHeight!!.value.asInt!!

                if (with * height > DEFAULT_BIG_BITMAP) {
                    val objectCounter =
                        updateClassObjectCounterMap(classObjectCounterMap, instanceClassId, true)
                    SwanLog.d(
                        TAG,
                        "suspect leak! bitmap name :${instance.instanceClassName} width: $with height: $height"
                    )
                    if (objectCounter.leakCount < SAME_CLASS_LEAK_OBJECT_PATH_THRESHOLD) {
                        leakingObjectIds.add(instance.objectId)
                        leakReasonTable[instance.objectId] =
                            "Bitmap size Over Threshold, $with x $height"

                        val leakObject = LeakObject(
                            className = instance.instanceClassName,
                            size = (with * height).toString(),
                            extDetail = "$with x $height",
                            objectId = (instance.objectId and 0xffffffffL).toString()
                        )

                        heapReport.leakObjects.add(leakObject)
                    }
                }
                continue
            }

            // native allocation/native allocation thunk/window
            if (nativeAllocationHeapClass?.objectId == superId1
                || nativeAllocationThunkHeapClass?.objectId == superId1
                || windowClass?.objectId == superId1
            ) {
                updateClassObjectCounterMap(classObjectCounterMap, instanceClassId, false)
            }
        }

        // 关注class和对应instance数量，加入 json
        for ((instanceId, objectCounter) in classObjectCounterMap) {
            val heapClass = graph.findObjectById(instanceId).asClass
            val classInfo = ClassInfo(
                className = heapClass?.name,
                instanceCount = objectCounter.allCounter.toString(),
            )
            heapReport.classInfos.add(classInfo)
        }

        // 查找基本类型数组
        val primitiveArrayIterator = graph.primitiveArrays.iterator()
        while (primitiveArrayIterator.hasNext()) {
            val primitiveArray = primitiveArrayIterator.next()
            val arraySize = primitiveArray.recordSize
            if (arraySize >= DEFAULT_BIG_PRIMITIVE_ARRAY) {
                val arrayName = primitiveArray.arrayClassName
                val typeName = primitiveArray.primitiveType.toString()
                SwanLog.d(
                    TAG,
                    "suspect leak! primitive array name $arrayName size $arraySize type name $typeName"
                )
                leakingObjectIds.add(primitiveArray.objectId)
                leakReasonTable[primitiveArray.objectId] =
                    "Primitive Array Size Over Threshold, $arraySize"
                val leakObject = LeakObject(
                    className = arrayName,
                    size = arraySize.toString(),
                    objectId = (primitiveArray.objectId and 0xffffffffL).toString(),
                    extDetail = "primitive array"
                )
                heapReport.leakObjects.add(leakObject)
            }
        }

        // 查找对象数组
        val objectArrayIterator = graph.objectArrays.iterator()
        while (objectArrayIterator.hasNext()) {
            val objectArray = objectArrayIterator.next()
            val arraySize = objectArray.recordSize
            if (arraySize >= DEFAULT_BIG_OBJECT_ARRAY) {
                val arrayName = objectArray.arrayClassName
                SwanLog.d(TAG, "object array name $arrayName object id: ${objectArray.objectId}")
                leakingObjectIds.add(objectArray.objectId)
                val leakObject = LeakObject(
                    className = arrayName,
                    size = arraySize.toString(),
                    objectId = (objectArray.objectId and 0xffffffffL).toString(),
                    extDetail = "object array"
                )
                heapReport.leakObjects.add(leakObject)
            }
        }

        val filterTime = System.currentTimeMillis()

        heapReport.runningInfo.filterInstanceTime =
            ((filterTime - startTime).toFloat() / 1000).toString()

        SwanLog.d(TAG, "filter leaking object spend ${heapReport.runningInfo.filterInstanceTime} s")

        val analyzer = HeapAnalyzer(processEventListener)
        analyzer.analyze(
            graph = graph,
            referenceMatchers = AndroidReferenceMatchers.appDefaults,
            leakingObjectIds = leakingObjectIds
        ).apply {
            // applicationLeaks
            for (applicationLeak in applicationLeaks) {
                SwanLog.d(
                    TAG,
                    "shortDescription: ${applicationLeak.shortDescription}, signature: ${applicationLeak.signature}, same leak size: ${applicationLeak.leakTraces.size}"
                )
                val (gcRootType, referencePath, leakTraceObject) = applicationLeak.leakTraces[0]
                val gcRoot = gcRootType.description
                val labels = leakTraceObject.labels.toTypedArray()
                leakTraceObject.leakingStatusReason =
                    leakReasonTable[leakTraceObject.objectId].toString()

                SwanLog.i(
                    TAG, "GC Root:" + gcRoot
                            + ", leakObjClazz:" + leakTraceObject.className
                            + ", leakObjType:" + leakTraceObject.typeName
                            + ", labels:" + labels.contentToString()
                            + ", leaking reason:" + leakTraceObject.leakingStatusReason
                            + ", leaking obj:" + (leakTraceObject.objectId and 0xffffffffL)
                )

                val gcPath = GCPath(
                    instanceCount = applicationLeak.leakTraces.size,
                    leakReason = leakTraceObject.leakingStatusReason,
                    gcRoot = gcRoot,
                    signature = applicationLeak.signature
                ).also {
                    heapReport.gcPaths.add(it)
                }

                for (reference in referencePath) {
                    val referenceName = reference.referenceName
                    val clazz = reference.originObject.className
                    val referenceDisplayName = reference.referenceDisplayName
                    val referenceGenericName = reference.referenceGenericName
                    val referenceType = reference.referenceType.toString()
                    val declaredClassName = reference.owningClassName

                    SwanLog.i(
                        TAG, "clazz:" + clazz +
                                ", referenceName:" + referenceName
                                + ", referenceDisplayName:" + referenceDisplayName
                                + ", referenceGenericName:" + referenceGenericName
                                + ", referenceType:" + referenceType
                                + ", declaredClassName:" + declaredClassName
                    )

                    val pathItem = PathItem(
                        reference = if (referenceDisplayName.startsWith("[")) {
                            clazz
                        } else {
                            "$clazz.$referenceDisplayName"
                        },
                        referenceType = referenceType,
                        declaredClass = declaredClassName
                    )

                    gcPath.paths.add(pathItem)
                }
                // 添加本身 trace path
                gcPath.paths.add(
                    PathItem(
                        reference = leakTraceObject.className,
                        referenceType = leakTraceObject.typeName
                    )
                )

            }

            SwanLog.d(TAG, "===================Library Leaks=========================")

            // libraryLeaks
            for (libraryLeak in libraryLeaks) {
                val (gcRootType, referencePath, leakTraceObject) = libraryLeak.leakTraces[0]
                val gcRoot = gcRootType.description
                val labels = leakTraceObject.labels.toTypedArray()
                leakTraceObject.leakingStatusReason =
                    leakReasonTable[leakTraceObject.objectId].toString()

                SwanLog.i(
                    TAG, "GC Root:" + gcRoot
                            + ", leakClazz:" + leakTraceObject.className
                            + ", labels:" + labels.contentToString()
                            + ", leaking reason:" + leakTraceObject.leakingStatusReason
                )

                val gcPath = GCPath(
                    instanceCount = libraryLeak.leakTraces.size,
                    leakReason = leakTraceObject.leakingStatusReason,
                    gcRoot = gcRoot,
                    signature = libraryLeak.signature
                ).also {
                    heapReport.gcPaths.add(it)
                }

                // 添加索引到的trace path
                for (reference in referencePath) {
                    val clazz = reference.originObject.className
                    val referenceName = reference.referenceName
                    val referenceDisplayName = reference.referenceDisplayName
                    val referenceGenericName = reference.referenceGenericName
                    val referenceType = reference.referenceType.toString()
                    val declaredClassName = reference.owningClassName

                    SwanLog.i(
                        TAG, "clazz:" + clazz +
                                ", referenceName:" + referenceName
                                + ", referenceDisplayName:" + referenceDisplayName
                                + ", referenceGenericName:" + referenceGenericName
                                + ", referenceType:" + referenceType
                                + ", declaredClassName:" + declaredClassName
                    )

                    val pathItem = PathItem(
                        reference = if (referenceDisplayName.startsWith("[")) {
                            clazz
                        } else {
                            "$clazz.$referenceDisplayName"
                        },
                        referenceType = referenceType,
                        declaredClass = declaredClassName
                    )

                    gcPath.paths.add(pathItem)

                }

                // 添加本身 trace path
                gcPath.paths.add(
                    PathItem(
                        reference = leakTraceObject.className,
                        referenceType = leakTraceObject.typeName
                    )
                )

                val analyzeTime = System.currentTimeMillis()
                heapReport.runningInfo.findGCPathTime =
                    ((analyzeTime - filterTime).toFloat() / 1000).toString()
                SwanLog.d(TAG, "find gc path spend ${heapReport.runningInfo.findGCPathTime} m")

            }


            val gson = Gson().toJson(heapReport)
            SwanLog.d(TAG, "Analyze result\n$gson")

            return HeapAnalysisSuccess(
                heapDumpFile,
                0,
                0,
                0,
                mapOf(),
                listOf(),
                listOf(),
                listOf()
            )

        }
    }


    private fun missingFileFailure(
        heapDumpFile: File
    ): HeapAnalysisFailure {
        val deletedReason = LeakDirectoryProvider.hprofDeleteReason(heapDumpFile)
        val exception = IllegalStateException(
            "Hprof file $heapDumpFile missing, deleted because: $deletedReason"
        )
        return HeapAnalysisFailure(
            heapDumpFile = heapDumpFile,
            createdAtTimeMillis = System.currentTimeMillis(),
            analysisDurationMillis = 0,
            exception = HeapAnalysisException(exception)
        )
    }

}

private fun updateClassObjectCounterMap(
    classObCountMap: MutableMap<Long, ObjectCounter>,
    instanceClassId: Long,
    isLeak: Boolean
): ObjectCounter {
    val objectCounter = classObCountMap[instanceClassId] ?: ObjectCounter().also {
        classObCountMap[instanceClassId] = it
    }

    objectCounter.allCounter++

    if (isLeak) {
        objectCounter.leakCount++
    }

    return objectCounter
}

data class ObjectCounter(var allCounter: Int = 0, var leakCount: Int = 0)