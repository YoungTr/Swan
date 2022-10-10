package com.bomber.swan.report

import org.json.JSONObject

/**
 * @author youngtr
 * @data 2022/4/9
 */
data class Issue(
    val type: Int = 0,
    var tag: String? = null,
    val key: String? = null,
    var content: JSONObject? = null,
    var files: MutableList<String> = mutableListOf()
) {

    companion object {
        const val ISSUE_REPORT_TYPE = "type"
        const val ISSUE_REPORT_TAG = "tag"
        const val ISSUE_REPORT_PROCESS = "process"
        const val ISSUE_REPORT_TIME = "time"

        const val ISSUE_LEAK_FOUND = 0
    }

    override fun toString(): String {
        return "tag[$tag]type[$type];key[$key];content[${content}];files[$files]"
    }
}
