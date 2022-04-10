package com.bomber.swan.util

/**
 * @author youngtr
 * @data 2022/4/10
 */
fun interface GcTrigger {

    fun runGc()

    object Default : GcTrigger {
        override fun runGc() {
            Runtime.getRuntime()
                .gc()
            enqueueReferences()
            System.runFinalization()


        }

        private fun enqueueReferences() {
            // Hack. We don't have a programmatic way to wait for the reference queue daemon to move
            // references to the appropriate queues.
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                throw AssertionError()
            }
        }
    }
}