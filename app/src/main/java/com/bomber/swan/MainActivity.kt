package com.bomber.swan

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityMainBinding
import com.bomber.swan.resource.ResourceActivity
import com.bomber.swan.util.SwanLog
import com.bomber.swan.util.newHandlerThread
import shark.HprofHeader
import shark.StreamingHprofReader
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val backgroundHandlerThread = newHandlerThread("Worker")
    private val backgroundHandler = Handler(backgroundHandlerThread.looper)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.sampleText.setOnClickListener {
            startActivity(Intent(this, ResourceActivity::class.java))
        }
        // Example of a call to a native method
//        binding.sampleText.text = stringFromJNI()

        binding.parseHprof.setOnClickListener {

            kotlin.runCatching {
                val hprofFile =
                    File("/data/user/0/com.bomber.swan/cache/swanresource/2022-04-17_20-32-25_885.hprof")


                backgroundHandler.post {
                    val hprofHeader = HprofHeader.parseHeaderOf(hprofFile)
                    SwanLog.d("MainActivity", "time: ${hprofHeader.heapDumpTimestamp}")

                    val streamingHprofReader = StreamingHprofReader.readerFor(hprofFile)
                    streamingHprofReader.readRecords()


                }
            }.onFailure { throwable ->
                SwanLog.d("MainActivity", "parse hprof fail: $throwable")
            }

        }
    }

    /**
     * A native method that is implemented by the 'swan' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'swan' library on application startup.
        init {
            System.loadLibrary("swan")
        }
    }
}