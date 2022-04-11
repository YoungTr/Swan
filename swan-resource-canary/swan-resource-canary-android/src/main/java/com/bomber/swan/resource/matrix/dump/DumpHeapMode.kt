package com.bomber.swan.resource.matrix.dump

sealed class DumpHeapMode {
    object NoDump : DumpHeapMode()
    object NormalDump : DumpHeapMode()
    object ForkDump : DumpHeapMode()
}