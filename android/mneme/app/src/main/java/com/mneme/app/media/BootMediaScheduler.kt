package com.mneme.app.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Reschedule lightweight periodic ingest after reboot (no heavy work here). */
class BootMediaScheduler : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        MediaIngestWorker.schedulePeriodic(context.applicationContext)
        Log.i("BootMediaScheduler", "periodic media work rescheduled")
    }
}
