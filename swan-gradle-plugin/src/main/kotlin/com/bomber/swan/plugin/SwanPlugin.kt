package com.bomber.swan.plugin

import com.android.build.gradle.AppExtension
import com.bomber.swan.javalib.util.Log
import com.bomber.swan.plugin.extension.SwanTraceExtension
import com.bomber.swan.plugin.task.SwanTaskManager
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project


class SwanPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val traceExtension = project.extensions.create("trace", SwanTraceExtension::class.java)

        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("Swan Plugin, Android Application plugin required")
        }

        Log.setLogLevel("D")

        SwanTaskManager.createSwanTasks(
            project.extensions.getByName("android") as AppExtension,
            project,
            traceExtension
        )

    }
}