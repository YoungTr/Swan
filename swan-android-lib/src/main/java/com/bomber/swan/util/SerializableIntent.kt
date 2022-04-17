package com.bomber.swan.util

import android.content.Intent
import java.io.Serializable

/**
 * @author youngtr
 * @data 2022/4/17
 */
class SerializableIntent(intent: Intent) : Serializable {
    private val uri = intent.toUri(0)

    @Transient
    private var _intent: Intent? = intent

    val intent: Intent
        get() = _intent.run {
            this ?: Intent.parseUri(uri, 0)
                .apply { _intent = this }
        }
}