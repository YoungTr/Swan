package com.bomber.swan.trace.core

interface BeatLifecycle {
    fun onStart()

    fun onStop()

    fun isAlive(): Boolean
}