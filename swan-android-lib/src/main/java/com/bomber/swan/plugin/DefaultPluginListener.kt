package com.bomber.swan.plugin

import com.bomber.swan.report.Issue
import com.bomber.swan.util.SwanLog

/**
 * @author youngtr
 * @data 2022/4/10
 */
object DefaultPluginListener : PluginListener {

    private const val TAG = "Swan.PluginListener"

    override fun onInit(plugin: Plugin) {
        SwanLog.i(TAG, "%s plugin is inited", plugin.getTag())
    }

    override fun onStart(plugin: Plugin) {
        SwanLog.i(TAG, "%s plugin is started", plugin.getTag())
    }

    override fun onStop(plugin: Plugin) {
        SwanLog.i(TAG, "%s plugin is stopped", plugin.getTag())
    }

    override fun onDestroy(plugin: Plugin) {
        SwanLog.i(TAG, "%s plugin is destroyed", plugin.getTag())
    }

    override fun onReportIssue(issue: Issue) {
        SwanLog.i(TAG, "report issue $issue")
    }
}