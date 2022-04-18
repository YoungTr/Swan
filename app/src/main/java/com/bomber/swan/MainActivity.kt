package com.bomber.swan

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityMainBinding
import com.bomber.swan.resource.ResourceActivity
import com.bomber.swan.util.SwanLog
import com.bomber.swan.util.newHandlerThread
import shark.HeapAnalyzer
import shark.HprofHeapGraph.Companion.openHeapGraph
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
                    File("/data/user/0/com.bomber.swan/cache/swanresource/2022-04-18_11-26-35_355.hprof")


                backgroundHandler.post {
//                    AndroidDebugHeapAnalyzer.runAnalysisBlocking(
//                        HeapDump(
//                            UUID.randomUUID().toString(),
//                            hprofFile,
//                            System.currentTimeMillis(),
//                            reason = "analyzer"
//                        )
//                    ) { process ->
//                        SwanLog.d("MainActivity", "process: ${process.progressPercent}")
//
//                    }


                    val heapGraph = hprofFile.openHeapGraph()
                    heapGraph.use { graph ->
                        val ACTIVITY_CLASS = "android.app.Activity"
                        val DESTROYED_FIELD_NAME = "mDestroyed"
                        val activityHeapClass = graph.findClassByName(ACTIVITY_CLASS)
                        for (instance in graph.instances) {
                            if (instance.isPrimitiveWrapper) continue
                            if (instance.instanceClassName.endsWith("ResourceActivity")) {
                                SwanLog.d(TAG, instance.instanceClassName)

                                val analyzer = HeapAnalyzer { step ->
                                    SwanLog.d(TAG, "step: ${step.name}")
                                }



                            }

                        }

                        SwanLog.d(TAG, "finished")

                    }


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

private const val TAG = "Swan.MainActivity"