package com.bomber.swan.trace.items

/**
 * @author youngtr
 * @data 2022/5/22
 */
data class MethodItem(val methodId: Int, var durTime: Int, val depth: Int, var count: Int = 1) {

    fun mergeMore(cost: Int) {
        count++
        durTime += cost
    }

    override fun toString(): String = "$depth,$methodId,$count,$durTime"

    fun print(): String {
        val inner = StringBuffer()
        for (i in 0 until depth) {
            inner.append('.')
        }
        return "$inner$methodId $count $durTime"
    }

}
