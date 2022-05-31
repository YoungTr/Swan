package com.bomber.swan.plugin.trace

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.CodeShrinker
import com.bomber.swan.javalib.util.Log
import com.bomber.swan.plugin.compat.CreationConfig.Companion.getCodeShrinker
import com.bomber.swan.plugin.extension.ITraceSwitchListener
import com.bomber.swan.plugin.extension.SwanTraceExtension
import com.bomber.swan.plugin.transform.SwanTraceTransform
import org.gradle.api.Project

object SwanTraceInjection : ITraceSwitchListener {
    private const val TAG = "Swan.TraceInjection"

    private var traceEnabled = false

    private var transform: SwanTraceTransform? = null

    override fun onTraceEnable(enabled: Boolean) {
        traceEnabled = enabled
    }

    fun inject(android: AppExtension, project: Project, traceExtension: SwanTraceExtension) {
        injectTransparentTransform(android, project, traceExtension)
        project.afterEvaluate {
            if (traceExtension.enable) {
                doInjection(android, project, traceExtension)
            }
        }
    }

    private fun doInjection(
        android: AppExtension,
        project: Project,
        traceExtension: SwanTraceExtension
    ) {
        android.applicationVariants.all { variant ->
            if (injectTaskOrTransform(project, traceExtension, variant) == InjectionMode.TransformInjection) {
                transformInjection()
            } else {
                taskInjection(project, traceExtension, variant)
            }
        }
    }

    private fun transformInjection() {
        Log.i(TAG, "Using trace transform mode.")
        transform!!.transparent = false
    }

    private fun taskInjection(
        project: Project,
        traceExtension: SwanTraceExtension,
        variant: BaseVariant
    ) {
        // todo
        Log.i(TAG, "Using trace task mode.")
    }


    private fun injectTransparentTransform(
        android: AppExtension,
        project: Project,
        traceExtension: SwanTraceExtension
    ) {
        transform = SwanTraceTransform(project, traceExtension)
        android.registerTransform(transform!!)
    }

    enum class InjectionMode {
        TaskInjection,
        TransformInjection
    }

    private fun injectTaskOrTransform(
        project: Project,
        extension: SwanTraceExtension,
        variant: BaseVariant
    ): InjectionMode {
        if (!variant.buildType.isMinifyEnabled
            || extension.transformInjectionForced
            || getCodeShrinker(project) == CodeShrinker.R8
        ) {
            return InjectionMode.TransformInjection
        }
        return InjectionMode.TaskInjection
    }

}