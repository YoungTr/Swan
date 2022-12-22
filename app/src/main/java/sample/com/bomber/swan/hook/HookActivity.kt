package sample.com.bomber.swan.hook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import dalvik.system.PathClassLoader
import sample.com.bomber.swan.R
import java.io.File

class HookActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hook)
        classLoader?.apply {
            val loader = this as PathClassLoader
            val path = loader.findLibrary("samplewan")
            Log.d(TAG, "lib swan path: $path")
            nativeHook(path)
        }


    }

    fun hook(view: View) {
        nativeWrite(File(filesDir, "open.txt").absolutePath, "Hello World!")
    }

    private external fun nativeHook(path: String)

    private external fun nativeWrite(path: String, content: String)

    companion object {
        private const val TAG = "Swan.Hook"

        fun start(context: Context) {
            context.startActivity(Intent(context, HookActivity::class.java))
        }
    }
}