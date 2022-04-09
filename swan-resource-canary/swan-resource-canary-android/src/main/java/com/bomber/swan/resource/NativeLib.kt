package com.bomber.swan.resource

class NativeLib {

    /**
     * A native method that is implemented by the 'resource' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'resource' library on application startup.
        init {
            System.loadLibrary("resource")
        }
    }
}