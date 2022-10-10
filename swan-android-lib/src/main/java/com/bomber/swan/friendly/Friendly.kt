package com.bomber.swan.friendly

import com.google.gson.Gson

val gson: Gson = Gson()

internal fun Any.toJson(): String = gson.toJson(this)