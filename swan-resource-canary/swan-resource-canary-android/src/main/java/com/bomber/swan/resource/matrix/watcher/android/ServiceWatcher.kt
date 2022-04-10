package com.bomber.swan.resource.matrix.watcher.android

import android.annotation.SuppressLint
import android.app.Service
import android.os.Handler
import android.os.IBinder
import com.bomber.swan.resource.matrix.friendly.checkMainThread
import com.bomber.swan.resource.matrix.watcher.ReachabilityWatcher
import com.bomber.swan.util.SwanLog
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author youngtr
 * @data 2022/4/10
 */
@SuppressLint("PrivateApi")
class ServiceWatcher(private val reachabilityWatcher: ReachabilityWatcher) : InstallableWatcher {

    private val serviceToBeDestroyed = WeakHashMap<IBinder, WeakReference<Service>>()

    private val activityThreadClass by lazy { Class.forName("android.app.ActivityThread") }

    private val activityThreadInstance by lazy {
        activityThreadClass.getDeclaredMethod("currentActivityThread").invoke(null)!!
    }

    private val activityThreadServices by lazy {
        val mServiceField =
            activityThreadClass.getDeclaredField("mServices").apply { isAccessible = true }


        // 获取 ActivityThread 成员变量 mServices
        @Suppress("UNCHECKED_CAST")
        mServiceField[activityThreadInstance] as Map<IBinder, Service>
    }

    private var uninstallActivityThreadHandlerCallback: (() -> Unit)? = null
    private var uninstallActivityManager: (() -> Unit)? = null

    override fun install() {
        checkMainThread()
        check(uninstallActivityManager == null) {
            "ServiceWatcher already installed"
        }

        try {

            // mCallback 为 ActivityThread 中 mH 中的 mCallback
            swapActivityThreadHandlerCallback { mCallback ->
                // 将原先的 mCallback 替换回去
                uninstallActivityThreadHandlerCallback = {
                    swapActivityThreadHandlerCallback { mCallback }
                }

                Handler.Callback { msg ->
                    if (msg.obj !is IBinder) {
                        return@Callback false
                    }

                    if (msg.what == STOP_SERVICE) {
                        val key = msg.obj as IBinder
                        activityThreadServices[key]?.let {
                            onServicePreDestroy(key, it)
                        }
                    }
                    mCallback?.handleMessage(msg) ?: false
                }
            }




        } catch (ignore: Throwable) {
            SwanLog.d(TAG, "Could not watch destroyed services")

        }

    }

    @SuppressLint("DiscouragedPrivateApi")
    /**
     * 替换 ActivityThread 中的 mH（Handler）中的 mCallback
     */
    private fun swapActivityThreadHandlerCallback(swap: (Handler.Callback?) -> Handler.Callback?) {
        val mHField =
            activityThreadClass.getDeclaredField("mH").apply { isAccessible = true }
        val mH = mHField[activityThreadInstance] as Handler

        val mCallbackField =
            Handler::class.java.getDeclaredField("mCallback").apply { isAccessible = true }
        val mCallback = mCallbackField[mH] as Handler.Callback?
        mCallbackField[mH] = swap(mCallback)
    }

    override fun uninstall() {
        TODO("Not yet implemented")
    }

    /**
     * service 将要执行了 onDestroy 方法
     */
    private fun onServicePreDestroy(token: IBinder, service: Service) {
        serviceToBeDestroyed[token] = WeakReference(service)
    }

    companion object {
        private const val TAG = "Swan.ServiceWatcher"
        private const val STOP_SERVICE = 116
        private const val METHOD_SERVICE_DONE_EXECUTING = "serviceDoneExecuting"
    }
}