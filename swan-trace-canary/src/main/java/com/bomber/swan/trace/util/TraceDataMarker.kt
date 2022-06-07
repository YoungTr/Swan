package com.bomber.swan.trace.util

import com.bomber.swan.trace.constants.DEFAULT_ANR
import com.bomber.swan.trace.constants.FILTER_STACK_KEY_ALL_PERCENT
import com.bomber.swan.trace.constants.TARGET_EVIL_METHOD_STACK
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

    /**
     * [100,0,1,1,2,2,3,3,4,4,5,5,0]
     */
    @JvmStatic
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

        for (item in result) {
            SwanLog.d(TAG, "$item")
        }

        val root = TreeNode(null, null)
        val count = stackToTree(result, root)
        SwanLog.i(TAG, "stackTree count: $count")
        result.clear()
        treeToStack(root, result)
    }

    fun isIn(trueId: Long): Boolean {
        return trueId.shr(63).and(0x1) == 1L
    }

    fun getMethodId(trueId: Long): Int {
        return trueId.shr(43).and(0xFFFFFL).toInt()
    }

    fun getTime(trueId: Long): Long {
        return trueId.and(0x7FFFFFFFFFFL)
    }

    /**
     * 将方法调用的 stack 结构调整为 tree 结构
     */
    private fun stackToTree(resultStack: LinkedList<MethodItem>, root: TreeNode): Int {
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

    /**
     * 将 tree 结构转换为真正的方法调用栈
     */
    private fun treeToStack(root: TreeNode, list: LinkedList<MethodItem>) {
        for (index in 0 until root.child.size) {
            val node = root.child[index] ?: continue
            if (node.item != null) {
                list.add(node.item)
            }
            if (node.child.isNotEmpty()) {
                treeToStack(node, list)
            }
        }
    }

    /**
     * 将 method item 压栈
     * 如果栈顶的 method id 与 [item] 的 method id 相同且 depth 也相同则将它们合并
     * 否则直接压入栈顶
     */
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

    /**
     * 裁剪堆栈信息
     * 最大的堆栈 count，默认30个
     * 将耗时太短的方法裁剪掉，[filter.isFilter(item.durTime.toLong(), filterCount)]
     */
    @JvmStatic
    fun trimStack(
        stack: LinkedList<MethodItem>,
        targetCount: Int = TARGET_EVIL_METHOD_STACK,
        filter: IStructuredDataFilter
    ) {
        if (targetCount < 0) {
            stack.clear()
            return
        }
        var filterCount = 1
        var curStackSize = stack.size
        while (curStackSize > targetCount) {
            val iterator = stack.listIterator(stack.size)
            while (iterator.hasPrevious()) {
                val item = iterator.previous()
                if (filter.isFilter(item.durTime.toLong(), filterCount)) {
                    iterator.remove()
                    curStackSize--
                    if (curStackSize <= targetCount) {
                        return
                    }
                }
            }
            curStackSize = stack.size
            filterCount++
            // 最多 filter 的次数，默认 60 次
            if (filter.filterMaxCount() < filterCount) {
                break
            }
        }
        val size = stack.size
        if (size > targetCount) {
            filter.fallback(stack, size)
        }
    }

    @JvmStatic
    fun stackToString(
        stack: LinkedList<MethodItem>,
        reportBuilder: StringBuilder,
        logcatBuilder: StringBuilder
    ): Int {
        logcatBuilder.append("|*\t\tTraceStack:").append("\n")
        logcatBuilder.append("|*\t\t[id count cost]").append("\n")
        val iterator = stack.iterator()
        var stackCost = 0
        while (iterator.hasNext()) {
            val item = iterator.next()
            reportBuilder.append(item.toString()).append("\n")
            logcatBuilder.append("|*\t\t").append(item.print()).append("\n")

            if (stackCost < item.durTime) {
                stackCost = item.durTime
            }
        }
        return stackCost
    }

    @JvmStatic
    fun getTreeKey(stack: LinkedList<MethodItem>, stackCost: Int): String {
        val ss = StringBuilder()
        val allLimit = (stackCost * FILTER_STACK_KEY_ALL_PERCENT).toLong()

        val sortList = LinkedList<MethodItem>()
        for (item in stack) {
            if (item.durTime >= allLimit) {
                sortList.add(item)
            }
        }

        sortList.sortWith { o1, o2 -> (o2.depth + 1) * o2.durTime.compareTo((o1.depth + 1) * o1.durTime) }
        if (sortList.isEmpty() && stack.isNotEmpty()) {
            val root = stack[0]
            sortList.add(root)
        } else if (sortList.size > 1 && sortList.peekFirst()!!.methodId == AppMethodBeat.METHOD_ID_DISPATCH) {
            sortList.removeFirst()
        }

        for (item in sortList) {
            ss.append("${item.methodId} |")
            break
        }
        return ss.toString()

    }
}

interface IStructuredDataFilter {
    fun isFilter(during: Long, filterCount: Int): Boolean
    fun filterMaxCount(): Int
    fun fallback(stack: LinkedList<MethodItem>, size: Int)
}

data class TreeNode(val item: MethodItem?, var father: TreeNode?) {
    val child: LinkedList<TreeNode?> = LinkedList()

    fun depth(): Int = item?.depth ?: 0
    fun add(node: TreeNode) = child.addFirst(node)
    fun isLeaf() = child.isEmpty()
}














