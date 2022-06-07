package sample.com.bomber.swan.trace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import sample.com.bomber.swan.databinding.ActivityTraceBinding
import java.lang.Thread.sleep

class TraceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTraceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTraceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val student = Student("Curry", 34)

    }


    fun textAnr(view: View) {
        funA()
    }

    fun funA() {
        funB()
        funC()
        funD()
        funE()
        funF()
        recursion(10)
    }

    fun funB() {
        sleep(500)
    }

    fun funC() {
        sleep(3000)
    }

    fun funD() {
        sleep(800)
    }

    fun funE() {
        sleep(2000)
    }

    fun funF() {
        sleep(50)
    }

    fun recursion(x: Int): Int {
        val result = if (x <= 1) 1 else recursion(x - 1) + x
        return result
    }

    companion object {
        private const val TAG = "Swan.TraceActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, TraceActivity::class.java))
        }
    }

    fun textEvil(view: View) {
        funB()
        funF()
    }

}