package com.bomber.swan.hooks

abstract class AbsHook {

    var status = Status.UN_COMMIT

    enum class Status {
        UN_COMMIT,
        COMMIT_SUCCESS,
        COMMIT_FAIL_ON_LOAD_LIB,
        COMMIT_FAIL_ON_CONFIGURE,
        COMMIT_FAIL_ON_HOOK
    }


    abstract fun getNativeLibraryName(): String

    abstract fun onConfigure(): Boolean

    abstract fun onHook(enableDebug: Boolean): Boolean

}