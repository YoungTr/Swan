package com.bomber.swan.resource

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityResourceBinding

class ResourceActivity : AppCompatActivity() {

    companion object {
        private var activityLeak: Activity? = null
    }

    private lateinit var binding: ActivityResourceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResourceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activityLeak = this

    }
}