package sample.com.bomber.swan

import android.app.Application

/**
 * @author youngtr
 * @data 2022/4/10
 */
class SwanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SwanInitializer.init(this)
    }
}