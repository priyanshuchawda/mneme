package com.mneme.app.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mneme.app.remind.DueReminderScheduler

/** Reschedule lightweight periodic ingest + due reminders after reboot. */
class BootMediaScheduler : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        MediaIngestWorker.schedulePeriodic(app)
        DueReminderScheduler.rescheduleUpcoming(app)
        Log.i("BootMediaScheduler", "periodic media + due reminders rescheduled")
    }
}
