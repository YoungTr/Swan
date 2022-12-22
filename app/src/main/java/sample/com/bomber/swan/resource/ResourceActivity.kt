package sample.com.bomber.swan.resource

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.hooks.HookManager
import com.bomber.swan.hooks.pthread.PthreadHook
import com.tencent.matrix.backtrace.WarmUpReporter.ReportEvent
import com.tencent.matrix.backtrace.WeChatBacktrace
import sample.com.bomber.swan.databinding.ActivityResourceBinding
import java.lang.Thread.sleep

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ResourceActivity : AppCompatActivity() {

    private var hasPrepared = false

    private var quite = false

    companion object {
        private const val TAG = "ResourceActivity"
        private var activityLeak: Activity? = null

        fun is64BitRuntime(): Boolean {
            val abi = Build.SUPPORTED_ABIS
            abi.forEach {
                if ("arm64-v8a".equals(it, true)
                    || "x86_64".equals(it, true)
                    || "mips64".equals(it, true)
                ) {
                    return true
                }
            }
            return false

        }

    }

    private var bitmap: Bitmap? = null


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

        bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        activityLeak = this

        binding.text.setOnClickListener {
            Thread {
                while (!quite) {
                    sleep(3000)
                    Log.d(TAG, "Thread: ${Thread.currentThread().name}")
                }
            }.start()
            nativeThread()
        }

        binding.threadHook.setOnClickListener {
            doHook()
        }

//        doHook()
    }

    private fun doHook() {
        if (hasPrepared) {
            return
        }
        hasPrepared = true
        backtraceInit()
        threadHook()
    }

    private fun backtraceInit() {
        WeChatBacktrace.setReporter { type, args ->
            if (type == ReportEvent.WarmedUp) {
                Log.i(TAG, String.format("WeChat QUT has warmed up."))
            } else if (type == ReportEvent.WarmUpDuration && args.size == 1) {
                Log.i(TAG, String.format("WeChat QUT Warm-up duration: %sms", args[0] as Long))
            }
        }

        if (is64BitRuntime()) {
            WeChatBacktrace.instance()
                .configure(application)
                .setBacktraceMode(WeChatBacktrace.Mode.Fp)
                .setQuickenAlwaysOn()
                .commit()
        } else {
            WeChatBacktrace.instance()
                .configure(application)
                .warmUpSettings(WeChatBacktrace.WarmUpTiming.PostStartup, 0)
                .directoryToWarmUp(WeChatBacktrace.getSystemFrameworkOATPath() + "boot.oat")
                .directoryToWarmUp(
                    WeChatBacktrace.getSystemFrameworkOATPath() + "boot-framework.oat"
                )
                .commit()
        }

        if (WeChatBacktrace.hasWarmedUp(this)) {
            warmedUpToast()
        }
    }

    private fun warmedUpToast() {
        binding.text.post {
            Toast.makeText(this, "Warm-up has been done!", Toast.LENGTH_LONG).show()
        }
    }

    private fun threadHook() {
        try {
            HookManager.INSTANCE
                .addHook(PthreadHook.INSTANCE)
                .commitHooks()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        quite = true
    }

    private external fun nativeThread()
}