package com.bomber.swan.android.common

class NativeLib {

    /**
     * A native method that is implemented by the 'common' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'common' library on application startup.
        init {
            System.loadLibrary("common")
        }
    }
}