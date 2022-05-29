package com.bomber.swan.sample

import android.app.Application

/**
 * @author youngtr
 * @data 2022/4/10
 */
class SwanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SwanInitializer.init(this)
    }
}