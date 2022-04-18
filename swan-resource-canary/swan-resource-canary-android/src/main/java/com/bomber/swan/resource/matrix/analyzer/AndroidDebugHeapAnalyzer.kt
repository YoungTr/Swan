package com.bomber.swan.resource.matrix.analyzer

import android.content.Intent
import com.bomber.swan.resource.matrix.EventListener.Event.*
import com.bomber.swan.resource.matrix.EventListener.Event.HeapAnalysisDone.HeapAnalysisSucceeded
import com.bomber.swan.resource.matrix.ResourceMatrixPlugin
import com.bomber.swan.resource.matrix.internal.LeakDirectoryProvider
import com.bomber.swan.util.SwanLog
import shark.*
import shark.HprofHeapGraph.Companion.openHeapGraph
import shark.OnAnalysisProgressListener.Step.PARSING_HEAP_DUMP
import java.io.File
import java.io.IOException

/**
 * @author youngtr
 * @data 2022/4/17
 */
object AndroidDebugHeapAnalyzer {

    private const val TAG = "Swan.HeapAnalyzer"
    private const val PROGUARD_MAPPING_FILE_NAME = "resourceCanaryObfuscationMapping.txt"

    private val application = ResourceMatrixPlugin.getApplication()

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
        val config = ResourceMatrixPlugin.config
        val heapAnalyzer = HeapAnalyzer(processEventListener)
        val proguardMappingReader = try {
            ProguardMappingReader(application.assets.open(PROGUARD_MAPPING_FILE_NAME))
        } catch (e: IOException) {
            null
        }

        processEventListener.onAnalysisProgress(PARSING_HEAP_DUMP)

        val sourceProvider = ConstantMemoryMetricsDualSourceProvider(
            ThrowingCancelableFileSourceProvider(heapDumpFile) {
                if (canceled()) {
                    throw RuntimeException("Analysis canceled")
                }
            })

        val closeableGraph = try {
            sourceProvider.openHeapGraph(proguardMapping = proguardMappingReader?.readProguardMapping())
        } catch (throwable: Throwable) {
            return HeapAnalysisFailure(
                heapDumpFile = heapDumpFile,
                createdAtTimeMillis = System.currentTimeMillis(),
                analysisDurationMillis = 0,
                exception = HeapAnalysisException(throwable)
            )
        }

        SwanLog.d(TAG, "graph: ${closeableGraph.instanceCount}")

        return closeableGraph
            .use { graph ->
                val result = heapAnalyzer.analyze(
                    heapDumpFile = heapDumpFile,
                    graph = graph,
                    leakingObjectFinder = config.leakingObjectFinder,
                    objectInspectors = config.objectInspectors,
                )

                SwanLog.d(TAG, "result: $result")

                if (result is HeapAnalysisSuccess) {
                    val lruCacheStats = (graph as HprofHeapGraph).lruCacheStats()
                    val randomAccessStats =
                        "RandomAccess[" +
                                "bytes=${sourceProvider.randomAccessByteReads}," +
                                "reads=${sourceProvider.randomAccessReadCount}," +
                                "travel=${sourceProvider.randomAccessByteTravel}," +
                                "range=${sourceProvider.byteTravelRange}," +
                                "size=${heapDumpFile.length()}" +
                                "]"
                    val stats = "$lruCacheStats $randomAccessStats"
                    result.copy(metadata = result.metadata + ("Stats" to stats))
                } else result
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