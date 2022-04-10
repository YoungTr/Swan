package com.bomber.swan.friendly

/**
 * @author youngtr
 * @data 2022/4/10
 */

object SwanLogExt {
    data class Ext(val name: String = "Tome " + System.currentTimeMillis())

    @JvmStatic
    @Volatile
    var config: Ext = Ext()
        set(newConfig) {
            val preConfig = field
            println("$field")
            field = newConfig
        }
}
