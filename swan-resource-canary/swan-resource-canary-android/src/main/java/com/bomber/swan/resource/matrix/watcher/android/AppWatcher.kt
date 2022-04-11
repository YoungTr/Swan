package com.bomber.swan.resource.matrix.watcher.android

import android.app.Application
import android.os.Handler
import com.bomber.swan.resource.friendly.checkMainThread
import com.bomber.swan.resource.matrix.watcher.ObjectWatcher
import com.bomber.swan.resource.matrix.watcher.ReachabilityWatcher
import com.bomber.swan.resource.matrix.watcher.android.fragment.FragmentAndViewModelWatcher
import com.bomber.swan.util.newHandlerThread
import java.util.concurrent.TimeUnit

/**
 * @author youngtr
 * @data 2022/4/10
 */
object AppWatcher {

    private const val RETAINED_DELAY_NOT_SET = -1L

    @Volatile
    var retainedDelayMillis = RETAINED_DELAY_NOT_SET
        private set

    private var installCause: Exception? = null

    private val handlerThread by lazy {
        newHandlerThread("swan_res", Thread.NORM_PRIORITY)
    }

    private val handler by lazy {
        Handler(handlerThread.looper)
    }

    private lateinit var watchersToInstall: List<InstallableWatcher>

    val objectWatcher = ObjectWatcher(
        clock = { System.currentTimeMillis() },
        checkRetainedExecutable = {
            handler.postDelayed(it, retainedDelayMillis)
        },
        isEnable = { true }
    )

    val isInstalled: Boolean
        get() = installCause != null


    @JvmOverloads
    fun init(
        application: Application,
        retainedDelayMillis: Long = TimeUnit.SECONDS.toMillis(5),
        watchersToInstall: List<InstallableWatcher> = appDefaultWatchers(application)
    ) {
        checkMainThread()
        if (isInstalled) {
            throw IllegalStateException(
                "AppWatcher already installed, see exception cause for prior install call",
                installCause
            )
        }
        check(retainedDelayMillis >= 0) {
            "retainedDelayMillis $retainedDelayMillis must be at least 0 ms"
        }
        installCause = RuntimeException("manualInstall() first called here")
        this.retainedDelayMillis = retainedDelayMillis

        this.watchersToInstall = watchersToInstall
    }

    fun start() {
        watchersToInstall.forEach {
            it.install()
        }
    }

    fun destroy() {
        if (isInstalled)
            watchersToInstall.forEach { it.uninstall() }
        handler.removeCallbacksAndMessages(null)
        handlerThread.quitSafely()
    }

    private fun appDefaultWatchers(
        application: Application,
        reachabilityWatcher: ReachabilityWatcher = objectWatcher
    ): List<InstallableWatcher> {
        return listOf(
            ActivityWatcher(application, reachabilityWatcher),
            FragmentAndViewModelWatcher(application, reachabilityWatcher),
            // TODO:  
            // RootViewWatcher(reachabilityWatcher),
            ServiceWatcher(reachabilityWatcher)
        )
    }
}