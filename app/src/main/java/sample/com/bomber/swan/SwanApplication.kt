package sample.com.bomber.swan

import android.app.Application
import android.os.Trace

/**
 * @author youngtr
 * @data 2022/4/10
 */
class SwanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Trace.beginSection("1")
        SwanInitializer.init(this)
        Trace.endSection()
    }
}