package com.emir.oructakibi

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.emir.oructakibi.data.AppDatabase
import com.emir.oructakibi.data.FastRecord
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val ACTION_MARK_FAST_TODAY = "com.emir.oructakibi.ACTION_MARK_FAST_TODAY"

class OrucWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == ACTION_MARK_FAST_TODAY) {
            runBlocking {
                val dao = AppDatabase.getInstance(context).fastRecordDao()
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                dao.upsert(FastRecord(date = today, type = 1))
            }
            updateOrucWidget(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_oruc)

            val launchIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            val markIntent = Intent(context, OrucWidgetProvider::class.java).apply {
                action = ACTION_MARK_FAST_TODAY
            }
            val markPending = PendingIntent.getBroadcast(
                context,
                101,
                markIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_mark_fast, markPending)

            views.setTextViewText(R.id.widget_title, "Oruç Rehberi")
            views.setTextViewText(R.id.widget_subtitle, "Bugünkü durumunu hızlıca işaretle")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

fun updateOrucWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, OrucWidgetProvider::class.java))
    if (ids.isNotEmpty()) {
        OrucWidgetProvider().onUpdate(context, manager, ids)
    }
}

