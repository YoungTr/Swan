package com.bomber.swan.hooks

import com.bomber.swan.util.SwanLog

class HookManager {

    companion object {
        val INSTANCE: HookManager = HookManager()

        private const val HOOK_COMMON_LIB_NAME = "swan-hookcommon"
        private const val TAG = "Swan.HookManager"
    }

    @Volatile
    private var hasNativeInitialized = false
    private val lock: Any = Any()

    private val pendingHooks = HashSet<AbsHook>(2)

    private var debug: Boolean = BuildConfig.DEBUG


    fun commitHooks() {
        synchronized(lock) {
            synchronized(pendingHooks) {
                if (pendingHooks.isEmpty()) {
                    return
                }
            }
            if (!hasNativeInitialized) {
                try {
                    System.loadLibrary(HOOK_COMMON_LIB_NAME)
                } catch (e: Throwable) {
                    SwanLog.printErrStackTrace(TAG, e, "")
                    return
                }

                if (!doPreHookInitializeNative(debug)) {
                    throw HookFailedException("Failed to do hook common pre-hook initialize.")
                }

                commitHooksLocked()

                doFinishInitializeNative(debug)

                hasNativeInitialized = true
            } else {
                commitHooksLocked()
            }
        }
    }

    private fun commitHooksLocked() {
        synchronized(pendingHooks) {
            for (hook in pendingHooks) {
                val libName = hook.getNativeLibraryName()
                if (libName.isEmpty()) {
                    continue
                }
                try {
                    System.loadLibrary(libName)
                } catch (e: Throwable) {
                    SwanLog.printErrStackTrace(TAG, e, "")
                    SwanLog.e(
                        TAG,
                        "Fail to load native library for %s, skip next steps.",
                        hook::class.java.name
                    )
                    hook.status = AbsHook.Status.COMMIT_FAIL_ON_LOAD_LIB
                }
            }
            for (hook in pendingHooks) {
                if (hook.status != AbsHook.Status.UN_COMMIT) {
                    SwanLog.e(
                        TAG,
                        "%s has failed steps before, skip calling onConfigure on it.",
                        hook::class.java
                    )
                    continue
                }
                if (!hook.onConfigure()) {
                    SwanLog.e(TAG, "Fail to configure %s, skip next steps", hook::class.java.name)
                    hook.status = AbsHook.Status.COMMIT_FAIL_ON_CONFIGURE
                }
            }
            for (hook in pendingHooks) {
                if (hook.status != AbsHook.Status.UN_COMMIT) {
                    SwanLog.e(
                        TAG,
                        "%s has failed steps before, skip calling onHook on it.",
                        hook::class.java.name
                    )
                    continue
                }
                if (hook.onHook(debug)) {
                    SwanLog.i(TAG, "%s is committed successfully.", hook::class.java.name)
                    hook.status = AbsHook.Status.COMMIT_SUCCESS
                } else {
                    SwanLog.e(TAG, "Fail to do hook in %s.", hook::class.java.name)
                    hook.status = AbsHook.Status.COMMIT_FAIL_ON_HOOK
                }
            }
            pendingHooks.clear()
        }
    }


    fun enableDebug(enable: Boolean): HookManager {
        this.debug = enable
        return this
    }

    fun addHook(hook: AbsHook): HookManager {
        if (hook.status != AbsHook.Status.COMMIT_SUCCESS) {
            synchronized(pendingHooks) {
                pendingHooks.add(hook)
            }
        }
        return this
    }

    fun clearHooks(): HookManager {
        synchronized(pendingHooks) {
            pendingHooks.clear()
        }
        return this
    }

    private external fun doPreHookInitializeNative(enableDebug: Boolean): Boolean

    private external fun doFinishInitializeNative(enableDebug: Boolean)

}

class HookFailedException(msg: String) : Exception(msg)