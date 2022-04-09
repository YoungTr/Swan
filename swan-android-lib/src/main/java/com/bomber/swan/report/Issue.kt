package com.bomber.swan.report

import com.bomber.swan.plugin.Plugin
import org.json.JSONObject

/**
 * @author youngtr
 * @data 2022/4/9
 */
data class Issue(
    val type: Int,
    var tag: String?,
    val key: String,
    var content: JSONObject?,
    var plugin: Plugin
) {

    companion object {
        const val ISSUE_REPORT_TYPE = "type"
        const val ISSUE_REPORT_TAG = "tag"
        const val ISSUE_REPORT_PROCESS = "process"
        const val ISSUE_REPORT_TIME = "time"
    }

    override fun toString(): String {
        return "tag[$tag]type[$type];key[$key];content[${content?.toString() ?: ""}]"
    }
}
