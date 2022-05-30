package com.bomber.swan.plugin.task

import com.android.build.gradle.AppExtension
import com.bomber.swan.plugin.extension.SwanTraceExtension
import com.bomber.swan.plugin.trace.SwanTraceInjection
import org.gradle.api.Project

object SwanTaskManager {
    const val TAG = "Swan.TaskManager"

    fun createSwanTasks(
        android: AppExtension,
        project: Project, traceExtension: SwanTraceExtension
    ) {

        createSwanTraceTask(android, project, traceExtension)

    }

    private fun createSwanTraceTask(
        android: AppExtension,
        project: Project,
        traceExtension: SwanTraceExtension
    ) {
        SwanTraceInjection.inject(android, project, traceExtension)
    }

}