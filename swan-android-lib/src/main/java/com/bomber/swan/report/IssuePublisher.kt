package com.bomber.swan.report

/**
 * @author youngtr
 * @data 2022/4/9
 */
internal class IssuePublisher(private val listener: OnIssueDetectListener) {
    private val publishMap = mutableSetOf<String>()

    fun publishIssue(issue: Issue) {
        listener.onDetectIssue(issue)
    }

    fun isPublished(key: String): Boolean {
        return publishMap.contains(key)
    }

    fun makePublished(key: String) {
        publishMap.add(key)
    }

    fun unMackPublished(key: String) {
        publishMap.remove(key)
    }

}

interface OnIssueDetectListener {
    fun onDetectIssue(issue: Issue)
}