package com.mneme.app.notif

import android.content.Context
import org.json.JSONArray

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
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        if (arr.length() > 0 && arr.getString(arr.length() - 1) == text) return
        arr.put(text)
        val trimmed = JSONArray()
        val start = (arr.length() - MAX).coerceAtLeast(0)
        for (i in start until arr.length()) trimmed.put(arr.getString(i))
        prefs.edit().putString(KEY, trimmed.toString()).apply()
    }

    @Synchronized
    fun drain(context: Context, limit: Int): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        if (arr.length() == 0) return emptyList()
        val take = minOf(limit, arr.length())
        val out = ArrayList<String>(take)
        for (i in 0 until take) out.add(arr.getString(i))
        val rest = JSONArray()
        for (i in take until arr.length()) rest.put(arr.getString(i))
        prefs.edit().putString(KEY, rest.toString()).apply()
        return out
    }

    @Synchronized
    fun size(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return JSONArray(prefs.getString(KEY, "[]")).length()
    }
}
