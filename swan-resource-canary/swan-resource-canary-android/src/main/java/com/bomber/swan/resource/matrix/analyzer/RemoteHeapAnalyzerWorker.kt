package com.bomber.swan.resource.matrix.analyzer

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteListenableWorker
import com.google.common.util.concurrent.ListenableFuture

/**
 * @author youngtr
 * @data 2022/4/17
 */
internal class RemoteHeapAnalyzerWorker(appContext: Context, workerParameters: WorkerParameters) :
    RemoteListenableWorker(appContext, workerParameters) {

    override fun startRemoteWork(): ListenableFuture<Result> {
        TODO("Not yet implemented")
    }
}