package com.bomber.swan.iocanary

class NativeLib {

    /**
     * A native method that is implemented by the 'iocanary' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'iocanary' library on application startup.
        init {
            System.loadLibrary("iocanary")
        }
    }
}