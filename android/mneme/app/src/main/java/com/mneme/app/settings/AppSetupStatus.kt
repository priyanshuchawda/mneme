package com.mneme.app.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.mneme.app.notif.MnemeNotificationListener

/**
 * Snapshot of setup permissions for Settings / first-run.
 * Pure status reads — UI / Activity owns request launchers.
 */
data class AppSetupStatus(
    val postNotifications: Boolean,
    val notifListener: Boolean,
    val mediaImages: Boolean,
    val microphone: Boolean,
    val exactAlarms: Boolean,
    val allFilesAccess: Boolean,
) {
    /** Needed for due reminders + ingest feedback. */
    val canNotify: Boolean get() = postNotifications

    /** Needed for WA / Gmail / SMS text ingest. */
    val canReadNotifs: Boolean get() = notifListener

    /** Needed for Scan WA / MediaStore watcher. */
    val canReadMedia: Boolean get() = mediaImages || allFilesAccess

    val missingCritical: Boolean
        get() = !canNotify || !canReadNotifs

    val readyForDailyUse: Boolean
        get() = canNotify && canReadNotifs && canReadMedia
}

object AppSetup {
    private const val PREFS = "retenqo_setup"
    private const val KEY_SEEN = "setup_screen_seen_v1"

    fun snapshot(context: Context): AppSetupStatus {
        val post = if (Build.VERSION.SDK_INT < 33) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
        val media = when {
            Build.VERSION.SDK_INT >= 33 ->
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES,
                ) == PackageManager.PERMISSION_GRANTED
            else ->
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
        }
        val mic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        val exact = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        } else {
            true
        }
        val allFiles = if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
        return AppSetupStatus(
            postNotifications = post,
            notifListener = MnemeNotificationListener.isEnabled(context),
            mediaImages = media,
            microphone = mic,
            exactAlarms = exact,
            allFilesAccess = allFiles,
        )
    }

    fun hasSeenSetup(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)

    fun markSetupSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEN, true)
            .apply()
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 31) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppDetails(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    fun openAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT < 30) return
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
