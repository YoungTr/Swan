package com.bomber.swan

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityMainBinding
import com.bomber.swan.resource.ResourceActivity
import com.bomber.swan.util.SwanLog
import com.bomber.swan.util.newHandlerThread
import shark.AndroidReferenceMatchers
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
                    File("/data/user/0/com.bomber.swan/cache/swanresource/2022-04-19_17-16-23_943.hprof")


                backgroundHandler.post {
                    val heapGraph = hprofFile.openHeapGraph()
                    heapGraph.use { graph ->
                        val ACTIVITY_CLASS = "android.app.Activity"
                        val DESTROYED_FIELD_NAME = "mDestroyed"
                        val activityHeapClass = graph.findClassByName(ACTIVITY_CLASS)
                        for (instance in graph.instances) {
                            if (instance.isPrimitiveWrapper) continue
                            if (instance.instanceClassName.endsWith("ResourceActivity")) {
                                SwanLog.d(TAG, instance.instanceClassName)


                                val mLeakingObjectIds = mutableSetOf<Long>()
                                mLeakingObjectIds.add(instance.objectId)

                                val analyzer =
                                    HeapAnalyzer { l -> SwanLog.d(TAG, "step: ${l.name}") }

                                val leakObjects: HeapAnalyzer.LeaksAndUnreachableObjects =
                                    analyzer.analyzeObjects(
                                        graph = graph,
                                        referenceMatchers = AndroidReferenceMatchers.appDefaults,
                                        leakingObjectIds = mLeakingObjectIds,
                                    )
                                SwanLog.d(
                                    TAG,
                                    "leak applicationLeaks: ${leakObjects.applicationLeaks.size}"
                                )
                                SwanLog.d(
                                    TAG,
                                    "leak libraryLeaks: ${leakObjects.libraryLeaks.size}"
                                )
                                SwanLog.d(
                                    TAG,
                                    "leak unreachableObjects: ${leakObjects.unreachableObjects.size}"
                                )

                                val leak = leakObjects.applicationLeaks[0]
                                val (gcRootType, referencePath, leakTraceObject) = leak.leakTraces[0]

                                val gcRoot = gcRootType.description
                                val labels = leakTraceObject.labels.toTypedArray()

                                SwanLog.d(
                                    TAG,
                                    "GC Root: $gcRoot, leakObjClazz: ${leakTraceObject.className}, leakObjType: ${leakTraceObject.typeName}"
                                )

                                SwanLog.d(TAG,"-------------reference---------------------------")
                                // 添加索引到的trace path
                                for (reference in referencePath) {
                                    val referenceName = reference.referenceName
                                    val clazz = reference.originObject.className
                                    val referenceDisplayName = reference.referenceDisplayName
                                    val referenceGenericName = reference.referenceGenericName
                                    val referenceType = reference.referenceType.toString()
                                    val declaredClassName = reference.owningClassName

                                    SwanLog.i(
                                        TAG, "clazz:" + clazz +
                                            ", referenceName:" + referenceName
                                            + ", referenceDisplayName:" + referenceDisplayName
                                            + ", referenceGenericName:" + referenceGenericName
                                            + ", referenceType:" + referenceType
                                            + ", declaredClassName:" + declaredClassName)

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