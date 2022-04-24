package com.bomber.swan.resource

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityResourceBinding

class ResourceActivity : AppCompatActivity() {

    companion object {
        private var activityLeak: Activity? = null
    }

    lateinit var bitmap: Bitmap

    private val intArray = mutableListOf<Int>()

    private val objArray = mutableListOf<Any>()

    private val intMap = mutableMapOf<String, Int>()

    private lateinit var binding: ActivityResourceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResourceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repeat(10000) {
            intArray.add(it)
            objArray.add("Int:$it")
            intMap["Int:$it"] = it
        }

        bitmap = BitmapFactory.decodeResource(resources, com.bomber.swan.R.mipmap.cat)

        activityLeak = this

    }
}