package com.bomber.swan.resource.matrix

import android.app.Application
import com.bomber.swan.plugin.Plugin
import com.bomber.swan.plugin.PluginListener
import com.bomber.swan.report.Issue
import com.bomber.swan.report.Issue.Companion.ISSUE_LEAK_FOUND
import com.bomber.swan.resource.matrix.config.IResultCallback
import com.bomber.swan.resource.matrix.config.ResourceConfig
import com.bomber.swan.resource.matrix.internal.InternalSwanResource
import com.bomber.swan.resource.matrix.watcher.android.AppWatcher

/**
 * @author youngtr
 * @data 2022/4/9
 */
class ResourceMatrixPlugin : Plugin() {

    @Volatile
    var config: ResourceConfig = ResourceConfig(resultCallback = result())

    override fun init(application: Application, pluginListener: PluginListener) {
        super.init(application, pluginListener)
        InternalSwanResource.config = config
        AppWatcher.init(application)

    }

    override fun start() {
        super.start()
        AppWatcher.start()
    }

    override fun stop() {
        super.stop()
    }

    override fun destroy() {
        super.destroy()
        AppWatcher.destroy()
    }

    private fun result(): IResultCallback {
        return IResultCallback { hprofFile, jsonFile, result ->
            val issue = Issue(ISSUE_LEAK_FOUND, content = result).apply {
                files.add(hprofFile)
                files.add(jsonFile)
            }
            onDetectIssue(issue)
        }
    }

}