package com.bomber.swan

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bomber.swan.databinding.ActivityMainBinding
import com.bomber.swan.resource.matrix.dump.ForkJvmHeapDumper
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.sampleText.setOnClickListener {
//            startActivity(Intent(this, ResourceActivity::class.java))
            val file = File(application.filesDir.absolutePath + File.separator + "heap.href")
            ForkJvmHeapDumper.dumpHeap(file)
        }
        // Example of a call to a native method
//        binding.sampleText.text = stringFromJNI()
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