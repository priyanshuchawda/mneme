package com.mneme.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mneme.app.MainActivity
import com.mneme.app.R

/** Compact, deterministic at-a-glance surface for the video/demo build. */
class RetenqoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onEnabled(context: Context) {
        updateAll(context)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, RetenqoWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { update(context, manager, it) }
        }

        internal fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_retenqo)
            val page = context.getSharedPreferences(RetenqoWidgetReceiver.PREFS, Context.MODE_PRIVATE)
                .getInt(RetenqoWidgetReceiver.KEY_PAGE, 0)
            val cards = listOf(
                listOf("Nexus · Fix export issue before Friday.", "Build Thursday · client tests Friday.", "3 PM review · prepare graphs."),
                listOf("ISRO · preprocessing + thresholds done.", "Tomorrow 4 PM · test-data review.", "Bring graphs + architecture diagram."),
                listOf("Fort · plan set for 4 PM.", "Metro route shared · pickup arranged.", "Assistant found a schedule clash."),
                listOf("Assistant · 4 PM conflict detected.", "ISRO review overlaps Fort plan.", "Suggestion: move Fort to 6 PM."),
            )[page % 4]
            views.setTextViewText(R.id.widget_summary_1, cards[0])
            views.setTextViewText(R.id.widget_summary_2, cards[1])
            views.setTextViewText(R.id.widget_summary_3, cards[2])
            val openBrief = PendingIntent.getActivity(
                    context,
                    id,
                    Intent(context, MainActivity::class.java).apply {
                        putExtra("pitch", "chat")
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            views.setOnClickPendingIntent(R.id.widget_root, openBrief)
            val next = PendingIntent.getBroadcast(
                context, id,
                Intent(context, RetenqoWidgetReceiver::class.java).setAction(RetenqoWidgetReceiver.ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_arrow, next)
            manager.updateAppWidget(id, views)
        }
    }
}
