package com.bomber.swan.sample.resource

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.sample.databinding.ActivityResourceBinding

class ResourceActivity : AppCompatActivity() {

    companion object {
        private var activityLeak: Activity? = null
        private var bitmap: Bitmap? = null

    }


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

        bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);

        activityLeak = this

        binding.text.setOnClickListener {

        }

    }
}