package com.bomber.swan.trace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityTraceBinding
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.trace.util.TraceDataMarker
import com.bomber.swan.util.SwanLog
import java.lang.Thread.sleep

class TraceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTraceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        AppMethodBeat.i(100)
        AppMethodBeat.getInstance().onStart()

        super.onCreate(savedInstanceState)
        binding = ActivityTraceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val record = AppMethodBeat.getInstance().maskIndex("trace_activity")
        funA()
        val data = AppMethodBeat.getInstance().copyData(record)
        SwanLog.d(TAG, "------Trace-------")
        for (id in data) {
            val isIn = TraceDataMarker.isIn(id)
            val methodId = TraceDataMarker.getMethodId(id)
            val time = TraceDataMarker.getTime(id)
            SwanLog.d(TAG, "method is ${if (isIn) "in" else "out"}, id: $methodId, time: $time")
        }
        AppMethodBeat.o(100)


    }

    fun funA() {
        AppMethodBeat.i(0)
        funB()
        funC()
        funD()
        funE()
        funF()
        AppMethodBeat.o(0)
    }

    fun funB() {
        AppMethodBeat.i(1)
        sleep(500)
        AppMethodBeat.o(1)
    }

    fun funC() {
        AppMethodBeat.i(2)
        sleep(300)
        AppMethodBeat.o(2)
    }

    fun funD() {
        AppMethodBeat.i(3)
        sleep(800)
        AppMethodBeat.o(3)
    }

    fun funE() {
        AppMethodBeat.i(4)
        sleep(200)
        AppMethodBeat.o(4)
    }

    fun funF() {
        AppMethodBeat.i(5)
        sleep(50)
        AppMethodBeat.o(5)
    }

    companion object {
        private const val TAG = "TraceActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, TraceActivity::class.java))
        }
    }
}