package com.bomber.swan.plugin.transform

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.builder.model.AndroidProject
import com.bomber.swan.plugin.extension.SwanTraceExtension
import com.bomber.swan.plugin.trace.Configuration
import com.google.common.base.Joiner
import org.gradle.api.Project
import java.io.File

class SwanTraceTransform(
    private val project: Project,
    private val traceExtension: SwanTraceExtension,
    var transparent: Boolean = true
) : Transform() {
    override fun getName(): String {
        return "SwanTraceTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean {
        return true
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        if (transparent) {
            transparent(transformInvocation)
        } else {
            transforming(transformInvocation)
        }
    }

    private fun transforming(transformInvocation: TransformInvocation) {
        val start = System.currentTimeMillis()

        val outProvider = transformInvocation.outputProvider
        val isIncremental = transformInvocation.isIncremental && this.isIncremental

        if (!isIncremental) {
            outProvider.deleteAll()
        }

    }

    private fun transparent(transformInvocation: TransformInvocation) {


    }

    private fun configure(transformInvocation: TransformInvocation): Configuration {

        val buildDir = project.buildDir.absolutePath
        val dirName = transformInvocation.context.variantName

        val mappingOut = Joiner.on(File.separatorChar).join(
            buildDir,
            AndroidProject.FD_OUTPUTS,
            "mapping",
            dirName
        )

        return Configuration.Builder()
            .setBaseMethodMap(traceExtension.baseMethodMapFile)
            .setBlockListFile(traceExtension.blackListFile)
            .setMethodMapFilePath("$mappingOut/methodMapping.txt")
            .setIgnoreMethodMapFilePath("$mappingOut/ignoreMethodMapping.txt")
            .setMappingPath(mappingOut)
            .setSkipCheckClass(traceExtension.skipCheckClass)
            .build()
    }
}