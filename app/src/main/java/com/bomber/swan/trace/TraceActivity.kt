package com.bomber.swan.trace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityTraceBinding
import com.bomber.swan.trace.core.AppMethodBeat

class TraceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTraceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        AppMethodBeat.getInstance().onStart()

        super.onCreate(savedInstanceState)
        binding = ActivityTraceBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, TraceActivity::class.java))
        }
    }
}