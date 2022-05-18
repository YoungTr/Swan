package com.bomber.swan.plugin

import com.bomber.swan.plugin.extension.SwanTraceExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project


class SwanPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val trace = project.extensions.create("trace", SwanTraceExtension::class.java)

        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("Swan Plugin, Android Application plugin required")
        }

        project.afterEvaluate {

        }

    }
}