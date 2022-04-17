package com.bomber.swan.resource.matrix.analyzer

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * @author youngtr
 * @data 2022/4/17
 */
internal class HeapAnalyzerWorker(appContext: Context, workerParameters: WorkerParameters) :
    Worker(appContext, workerParameters) {

    override fun doWork(): Result {


        return Result.success()
    }
}