package com.bomber.swan.resource.matrix.dumper

sealed class DumpHeapMode {
    object NoDump : DumpHeapMode()
    object NormalDump : DumpHeapMode()
    object ForkDump : DumpHeapMode()
}