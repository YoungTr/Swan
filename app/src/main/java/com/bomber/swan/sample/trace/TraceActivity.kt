package com.bomber.swan.sample.trace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityTraceBinding
import com.bomber.swan.trace.constants.FILTER_STACK_MAX_COUNT
import com.bomber.swan.trace.constants.TARGET_EVIL_METHOD_STACK
import com.bomber.swan.trace.constants.TIME_UPDATE_CYCLE_MS
import com.bomber.swan.trace.core.AppMethodBeat
import com.bomber.swan.trace.items.MethodItem
import com.bomber.swan.trace.util.IStructuredDataFilter
import com.bomber.swan.trace.util.TraceDataMarker
import com.bomber.swan.util.SwanLog
import java.lang.Thread.sleep
import java.util.*

class TraceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTraceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        AppMethodBeat.i(AppMethodBeat.METHOD_ID_DISPATCH)
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

        val stack = LinkedList<MethodItem>()
        TraceDataMarker.structuredDataToStack(data, stack, true, SystemClock.uptimeMillis())
        TraceDataMarker.trimStack(
            stack,
            TARGET_EVIL_METHOD_STACK,
            object : IStructuredDataFilter {
                override fun isFilter(during: Long, filterCount: Int): Boolean {
                    return during < filterCount * TIME_UPDATE_CYCLE_MS
                }

                override fun filterMaxCount(): Int {
                    return FILTER_STACK_MAX_COUNT
                }

                override fun fallback(stack: LinkedList<MethodItem>, size: Int) {
                    SwanLog.w(
                        TAG,
                        "[fallback] size: %s, target size: $TARGET_EVIL_METHOD_STACK, stack: $stack"
                    )
                    val iterator =
                        stack.listIterator(size.coerceAtMost(TARGET_EVIL_METHOD_STACK))
                    while (iterator.hasNext()) {
                        iterator.next()
                        iterator.remove()
                    }
                }

            })


        /**
         * rest for recursion
         *
        val record2 = AppMethodBeat.getInstance().maskIndex("trace_recursion")
        val recursion = recursion(10)
        SwanLog.d(TAG, "recursion: $recursion")
        val data2 = AppMethodBeat.getInstance().copyData(record2)
        SwanLog.d(TAG, "data2: ${data2.contentToString()}")

        SwanLog.d(TAG, "------Recursion-------")
        for (id in data2) {
        val isIn = TraceDataMarker.isIn(id)
        val methodId = TraceDataMarker.getMethodId(id)
        val time = TraceDataMarker.getTime(id)
        SwanLog.d(TAG, "method is ${if (isIn) "in" else "out"}, id: $methodId, time: $time")
        }
         **/

        AppMethodBeat.o(AppMethodBeat.METHOD_ID_DISPATCH)

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

    fun recursion(x: Int): Int {
        AppMethodBeat.i(6)
        val result = if (x <= 1) 1 else recursion(x - 1) + x
        AppMethodBeat.o(6)
        return result
    }

    companion object {
        private const val TAG = "Swan.TraceActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, TraceActivity::class.java))
        }
    }
}