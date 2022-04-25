package com.bomber.swan.util

sealed class SizeUnit {
    abstract fun toByte(value: Long): Float
    abstract fun toKB(value: Long): Float
    abstract fun toMB(value: Long): Float

    abstract fun toByte(value: Int): Float
    abstract fun toKB(value: Int): Float
    abstract fun toMB(value: Int): Float

    object BYTE : SizeUnit() {
        override fun toByte(value: Long) = value.toFloat()

        override fun toKB(value: Long) = value / 1024.0f

        override fun toMB(value: Long) = value / 1024.0f / 1024.0f

        override fun toByte(value: Int) = value.toFloat()

        override fun toKB(value: Int) = value / 1024.0f

        override fun toMB(value: Int) = value / 1024.0f / 1024.0f
    }

    object KB : SizeUnit() {
        override fun toByte(value: Long) = value * 1024.0f

        override fun toKB(value: Long) = value.toFloat()

        override fun toMB(value: Long) = value / 1024.0f

        override fun toByte(value: Int) = value * 1024.0f

        override fun toKB(value: Int) = value.toFloat()

        override fun toMB(value: Int) = value / 1024.0f
    }

    object MB : SizeUnit() {
        override fun toByte(value: Long) = value * 1024.0f * 1024.0f

        override fun toKB(value: Long) = value * 1024.0f

        override fun toMB(value: Long) = value.toFloat()

        override fun toByte(value: Int) = value * 1024.0f * 1024.0f

        override fun toKB(value: Int) = value * 1024.0f

        override fun toMB(value: Int) = value.toFloat()
    }
}