package com.mneme.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RetenqoWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NEXT) return
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_PAGE, (prefs.getInt(KEY_PAGE, 0) + 1) % 4).apply()
        RetenqoWidgetProvider.updateAll(app)
    }

    companion object {
        const val ACTION_NEXT = "com.mneme.app.widget.NEXT"
        const val PREFS = "retenqo_widget"
        const val KEY_PAGE = "page"
    }
}
