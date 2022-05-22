package com.bomber.swan.trace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityTraceBinding
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.util.SwanLog
import java.util.*

class TraceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTraceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        AppMethodBeat.getInstance().onStart()

        super.onCreate(savedInstanceState)
        binding = ActivityTraceBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val linkedList = LinkedList<Int>()
        linkedList.push(0)
        linkedList.push(1)
        linkedList.push(2)
        linkedList.push(3)
        linkedList.push(4)
        linkedList.push(5)

        repeat(linkedList.size) {
            SwanLog.d(TAG, linkedList.pop().toString())
        }

    }

    companion object {
        private const val TAG = "TraceActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, TraceActivity::class.java))
        }
    }
}