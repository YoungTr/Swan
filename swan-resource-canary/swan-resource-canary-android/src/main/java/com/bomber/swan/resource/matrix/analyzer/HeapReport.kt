package com.bomber.swan.resource.matrix.analyzer

/**
 * @author youngtr
 * @data 2022/4/25
 */
data class HeapReport(
    var runningInfo: RunningInfo,
    val gcPaths: MutableList<GCPath> = mutableListOf(),
    // 发生泄漏的类
    val classInfos: MutableList<ClassInfo> = mutableListOf(),
    // 发生泄漏的大对象
    val leakObjects: MutableList<LeakObject> = mutableListOf(),
    var analysisDone: Boolean = false,
    var reAnalysisTimes: Int = 0
)


data class RunningInfo(
    // jvm info
    val jvmMax: String,
    val jvmUsed: String,
    // memory info
    val vss: String,
    val pss: String,
    val threadCount: String? = null,
    val fdCount: String,
    val threadList: MutableList<String> = mutableListOf(),
    val fdList: MutableList<String> = mutableListOf(),
    // device info
    val sdkInt: String,
    val manufacture: String,
    val buildModel: String,
    // app info
    val appVersion: String,
    val currentPage: String,
    val usageSeconds: String,
    val nowTime: String,
    val deviceMemTotal: String,
    val deviceMemAvailable: String,

    val dumpReason: String,
    val analysisReason: String,

    // perf data
    var filterInstanceTime: String? = null,
    var findGCPathTime: String? = null

)

data class GCPath(
    val instanceCount: Int, val leakReason: String,
    val gcRoot: String, val signature: String,
    val paths: MutableList<PathItem> = mutableListOf()
)

data class PathItem(
    val reference: String,
    val referenceType: String,
    val declaredClass: String? = null
)

data class ClassInfo(
    val className: String?,
    val instanceCount: String,
    var leakInstanceCount: String? = null
)

data class LeakObject(
    val className: String,
    val size: String,
    val objectId: String,
    val extDetail: String
)