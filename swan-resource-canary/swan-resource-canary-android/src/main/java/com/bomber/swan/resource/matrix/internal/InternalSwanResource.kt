package com.bomber.swan.resource.matrix.internal

import android.app.Application
import com.bomber.swan.resource.matrix.watcher.OnObjectRetainedListener
import com.bomber.swan.resource.matrix.watcher.android.AppWatcher
import com.bomber.swan.util.GcTrigger

/**
 * @author youngtr
 * @data 2022/4/10
 */
object InternalSwanResource : OnObjectRetainedListener {

    @Suppress("ObjectPropertyName")
    private lateinit var _application: Application

    val application: Application
        get() {
            check(::_application.isInitialized)
            return _application
        }

    override fun onObjectRetained() {
        TODO("Not yet implemented")
    }


    fun init(application: Application) {
        _application = application

        checkRunningInDebuggableBuild()

        AppWatcher.objectWatcher.addOnObjectRetainedListener(this)

        val gcTrigger = GcTrigger.Default



    }

    private fun checkRunningInDebuggableBuild() {

    }


}