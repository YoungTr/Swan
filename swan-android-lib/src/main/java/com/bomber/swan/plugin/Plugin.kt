package com.bomber.swan.plugin

import android.app.Application
import com.bomber.swan.listeners.IAppForeground
import com.bomber.swan.report.Issue
import com.bomber.swan.report.OnIssueDetectListener
import com.bomber.swan.util.getProcessName

/**
 * @author youngtr
 * @data 2022/4/9
 */
abstract class Plugin : IPlugin, IAppForeground, OnIssueDetectListener {

    companion object {
        private const val TAG = "Swam.Plugin"
        const val PLUGIN_CREATE = 0x00
        const val PLUGIN_INITED = 0x01
        const val PLUGIN_STARTED = 0x02
        const val PLUGIN_STOPPED = 0x04
        const val PLUGIN_DESTROYED = 0x08
    }

    val isPluginStarted: Boolean
        get() = status == PLUGIN_STARTED
    val isPluginStopped: Boolean
        get() = status == PLUGIN_STOPPED
    val isPluginDestroyed: Boolean
        get() = status == PLUGIN_DESTROYED
    var status = PLUGIN_CREATE
        private set


    private lateinit var app: Application
    private lateinit var pluginListener: PluginListener

    override fun init(application: Application, pluginListener: PluginListener) {
        this.app = application
        status = PLUGIN_INITED
        this.pluginListener = pluginListener
    }

    override fun onDetectIssue(issue: Issue) {
        if (issue.tag.isNullOrEmpty()) {
            issue.tag = getTag()
        }
        issue.plugin = this
        val content = issue.content
        content?.apply {
            put(Issue.ISSUE_REPORT_TAG, issue.tag)
            if (issue.type != 0)
                put(Issue.ISSUE_REPORT_TYPE, issue.type)
            put(Issue.ISSUE_REPORT_PROCESS, getProcessName(app))
            put(Issue.ISSUE_REPORT_TIME, System.currentTimeMillis())

        }

        pluginListener.onReportIssue(issue)
    }


    override fun onForeground(isForeground: Boolean) {

    }

    override fun getApplication(): Application {
        return app
    }

    override fun start() {
        if (isPluginDestroyed) throw RuntimeException("plugin start, but plugin has been already destroyed")

        if (isPluginStarted) throw RuntimeException("plugin start, but plugin has been already started")

        status = PLUGIN_STARTED

        pluginListener.onStart(this)
    }

    override fun stop() {
        if (isPluginDestroyed) throw RuntimeException("plugin stop, but plugin has been already destroyed")

        if (!isPluginStarted) throw RuntimeException("plugin stop, but plugin is never started")

        status = PLUGIN_STOPPED

        pluginListener.onStop(this)

    }

    override fun destroy() {
        // stop first
        if (isPluginStarted) stop()
        if (isPluginDestroyed) throw RuntimeException("plugin destroy, but plugin has been already destroyed")
        pluginListener.onDestroy(this)
    }

}