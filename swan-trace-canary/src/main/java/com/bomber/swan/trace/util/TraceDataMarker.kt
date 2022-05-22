package com.bomber.swan.trace.util

import com.bomber.swan.trace.constants.DEFAULT_ANR
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.trace.items.MethodItem
import com.bomber.swan.util.SwanLog
import java.util.*

/**
 * @author youngtr
 * @data 2022/5/22
 */
object TraceDataMarker {

    private const val TAG = "Swan.TraceData"

    fun structuredDataToStack(
        buffer: LongArray,
        result: LinkedList<MethodItem>,
        isStrict: Boolean,
        endTime: Long
    ) {
        var lastInId: Int
        var depth = 0

        val rawData = LinkedList<Long>()
        var isBegin = !isStrict
        for (trueId in buffer) {
            if (trueId == 0L) continue
            val isIn = isIn(trueId)
            val methodId = getMethodId(trueId)
            if (isStrict) {
                if (isIn && AppMethodBeat.METHOD_ID_DISPATCH == methodId) {
                    isBegin = true
                }
                if (!isBegin) {
                    SwanLog.d(TAG, "never begin! pass this method[%s]", methodId)
                    continue
                }

            }
            if (isIn) {
                lastInId = methodId
                if (lastInId == AppMethodBeat.METHOD_ID_DISPATCH) {
                    depth = 0
                }
                depth++
                rawData.push(trueId)
            } else {
                @Suppress("UnnecessaryVariable")
                val outMethodId = methodId
                if (rawData.isNotEmpty()) {
                    var inId = rawData.pop()
                    depth--
                    var inMethodId: Int
                    val tmp = LinkedList<Long>()
                    tmp.add(inId)
                    while ((getMethodId(inId).apply {
                            inMethodId = this
                        }) != outMethodId && rawData.isNotEmpty()) {
                        inId = rawData.pop()
                        depth--
                        tmp.add(inId)
                    }
                    if (inMethodId != outMethodId && inMethodId == AppMethodBeat.METHOD_ID_DISPATCH) {
                        rawData.addAll(tmp)
                        depth += rawData.size
                        continue
                    }

                    val outTime = getTime(trueId)
                    val inTime = getTime(inId)
                    val during = outTime - inTime
                    SwanLog.d(TAG, "method: $methodId, cast: $during")
                    if (during < 0) {
                        SwanLog.e(TAG, "[structureDataToStack] trace during invalid:%d", during)
                        rawData.clear()
                        result.clear()
                        return
                    }
                    val methodItem = MethodItem(outMethodId, during.toInt(), depth)
                    addMethodItem(result, methodItem)

                }
            }
        }

        while (rawData.isNotEmpty() && isStrict) {
            val trueId = rawData.pop()
            val methodId = getMethodId(trueId)
            val isIn = isIn(trueId)
            val inTime = getTime(trueId) + AppMethodBeat.getDiffTime()
            if (!isIn) {
                continue
            }
            val methodItem = MethodItem(methodId, (endTime - inTime).toInt(), rawData.size)
            addMethodItem(result, methodItem)
        }

        val root = TreeNode(null, null)
        val count = stackToTree(result, root)
        SwanLog.i(TAG, "stackTree count: $count")
        result.clear()
        treeToStack(root, result)
    }

    private fun isIn(trueId: Long): Boolean {
        return trueId.shr(63).and(0x1) == 1L
    }

    private fun getMethodId(trueId: Long): Int {
        return trueId.shr(43).and(0xFFFFFL).toInt()
    }

    private fun getTime(trueId: Long): Long {
        return trueId.and(0x7FFFFFFFFFFL)
    }

    fun stackToTree(resultStack: LinkedList<MethodItem>, root: TreeNode): Int {
        var lastNode: TreeNode? = null
        val iterator = resultStack.listIterator(0)
        var count = 0
        while (iterator.hasNext()) {
            val node = TreeNode(iterator.next(), lastNode)
            count++
            if (lastNode == null && node.depth() != 0) {
                SwanLog.e(TAG, "[stackToTree] begin error! why the first node's depth is not 0!")
                return 0
            }
            val depth = node.depth()
            if (lastNode == null || depth == 0) {
                root.add(node)
            } else if (lastNode.depth() >= depth) {
                while (lastNode != null && lastNode.depth() > depth) {
                    lastNode = lastNode.father
                }
                if (lastNode?.father != null) {
                    node.father = lastNode.father
                    lastNode.father!!.add(node)
                }
            } else {
                lastNode.add(node)
            }
            lastNode = node
        }

        return count
    }

    private fun treeToStack(root: TreeNode, list: LinkedList<MethodItem>) {
        for (index in 0..root.child.size) {
            val node = root.child[index] ?: continue
            if (node.item != null) {
                list.add(node.item)
            }
            if (node.child.isNotEmpty()) {
                treeToStack(node, list)
            }
        }
    }

    private fun addMethodItem(resultStack: LinkedList<MethodItem>, item: MethodItem): Int {
        var last: MethodItem? = null
        if (resultStack.isNotEmpty()) {
            last = resultStack.peek()
        }

        return if (last != null && last.methodId == item.methodId && last.depth == item.depth) {
            item.durTime = if (item.durTime == DEFAULT_ANR) last.durTime else item.durTime
            last.mergeMore(item.durTime)
            last.durTime
        } else {
            resultStack.push(item)
            item.durTime
        }
    }

    fun trimStack(stack: MutableList<MethodItem>, targetCount: Int, filter: IStructuredDataFilter) {
        if (targetCount < 0) {
            stack.clear()
            return
        }
        // TODO()
    }

}

interface IStructuredDataFilter {
    fun isFilter(during: Long, filterCount: Int): Boolean
    fun filterMaxCount(): Int
    fun fallback(stack: List<MethodItem>, size: Int)
}

data class TreeNode(val item: MethodItem?, var father: TreeNode?) {
    val child: LinkedList<TreeNode?> = LinkedList()

    fun depth(): Int = item?.depth ?: 0
    fun add(node: TreeNode) = child.addFirst(node)
    fun isLeaf() = child.isEmpty()
}














