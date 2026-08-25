package com.mneme.app.notif

import android.content.Context

/**
 * Tiny durable queue for notification texts (no Room schema churn).
 * Listener only appends; [NotifIngestWorker] drains.
 */
object NotifIngestQueue {
    private const val PREFS = "mneme_notif_queue"
    private const val KEY = "pending"
    private const val MAX = 12

    @Synchronized
    fun offer(context: Context, text: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = NotifQueueLogic.offer(prefs.getString(KEY, "[]") ?: "[]", text, MAX)
        prefs.edit().putString(KEY, next).apply()
    }

    @Synchronized
    fun drain(context: Context, limit: Int): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val (batch, rest) = NotifQueueLogic.drain(prefs.getString(KEY, "[]") ?: "[]", limit)
        prefs.edit().putString(KEY, rest).apply()
        return batch
    }

    @Synchronized
    fun size(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return NotifQueueLogic.size(prefs.getString(KEY, "[]") ?: "[]")
    }
}
