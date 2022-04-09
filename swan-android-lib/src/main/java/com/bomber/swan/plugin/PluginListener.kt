package com.bomber.swan.plugin

import com.bomber.swan.report.Issue

/**
 * @author youngtr
 * @data 2022/4/9
 */
interface PluginListener {

    fun onInit(plugin: Plugin)

    fun onStart(plugin: Plugin)

    fun onStop(plugin: Plugin)

    fun onDestroy(plugin: Plugin)

    fun onReportIssue(issue: Issue)
}