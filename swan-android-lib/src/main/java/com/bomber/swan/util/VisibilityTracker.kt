package com.bomber.swan.util

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter

class VisibilityTracker(private val listener: (Boolean) -> Unit) :
    Application.ActivityLifecycleCallbacks by noOpDelegate(), BroadcastReceiver() {

    private var startedActivityCount = 0

    /**
     * Visible activities are any activity started but not stopped yet. An activity can be paused
     * yet visible: this will happen when another activity shows on top with a transparent background
     * and the activity behind won't get touch inputs but still need to render / animate.
     */
    private var hasVisibleActivities: Boolean = false

    private var screenOn: Boolean = true

    private var lastUpdate: Boolean = false

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        if (!hasVisibleActivities && startedActivityCount == 1) {
            hasVisibleActivities = true
            updateVisible()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        if (startedActivityCount > 0) {
            startedActivityCount--
        }
        if (hasVisibleActivities && startedActivityCount == 0 && !activity.isChangingConfigurations) {
            hasVisibleActivities = false
            updateVisible()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        screenOn = intent.action != ACTION_SCREEN_OFF
        updateVisible()
    }

    private fun updateVisible() {
        val visible = screenOn && hasVisibleActivities
        if (visible != lastUpdate) {
            lastUpdate = visible
            listener.invoke(visible)
        }
    }

}

fun Application.registerVisibilityListener(listener: (Boolean) -> Unit) {
    val visibilityTrack = VisibilityTracker(listener)
    registerActivityLifecycleCallbacks(visibilityTrack)
    registerReceiver(visibilityTrack, IntentFilter().apply {
        addAction(ACTION_SCREEN_OFF)
        addAction(ACTION_SCREEN_ON)
    })
}