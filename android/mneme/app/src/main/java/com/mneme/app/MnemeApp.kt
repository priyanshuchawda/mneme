package com.mneme.app

import android.app.Application
import android.util.Log
import com.mneme.app.media.MediaIngestWorker
import com.mneme.app.media.MediaStoreTrigger
import com.mneme.app.remind.DueReminderScheduler

class MnemeApp : Application() {
    lateinit var mediaTrigger: MediaStoreTrigger
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        DueReminderScheduler.ensureChannel(this)
        // Periodic + opportunistic MediaStore hints — no always-on OCR/LLM.
        MediaIngestWorker.schedulePeriodic(this)
        mediaTrigger = MediaStoreTrigger(this).also { it.start() }
        DueReminderScheduler.rescheduleUpcoming(this)
        Log.i(TAG, "MnemeApp ready (battery-aware media + due reminders)")
    }

    companion object {
        private const val TAG = "MnemeApp"
        lateinit var instance: MnemeApp
            private set
    }
}
