package com.ahm.mytime

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import android.widget.RemoteViews
import java.util.*

class PrayerTimeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Re-schedule the midnight alarm upon widget update (e.g., device reboot or new widget)
        MidnightAlarmScheduler.scheduleMidnightUpdate(context)

        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

// A private data class to map prayer slot data to the specific views in this widget's layout
private data class PrayerSlotViews(val iconViewId: Int, val nameViewId: Int, val timeViewId: Int)

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.widget_prayer_time)
    val prefs = getPrefs(context)

    // Update date information
    views.setTextViewText(R.id.tv_day, getTodayDayName())
    views.setTextViewText(R.id.tv_date, getTodayGregorianDate())
    views.setTextViewText(R.id.tv_islamic_date_location, getTodayHijriDate(prefs))

    // Map shared prayer slot info to the specific views in this widget layout
    val prayerViewMappings = mapOf(
        "fajr" to PrayerSlotViews(R.id.iv_fajr_icon, R.id.tv_fajr_name, R.id.tv_fajr_time),
        "sunrise" to PrayerSlotViews(R.id.iv_sunrise_icon, R.id.tv_sunrise_name, R.id.tv_sunrise_time),
        "dhuhr" to PrayerSlotViews(R.id.iv_dhuhr_icon, R.id.tv_dhuhr_name, R.id.tv_dhuhr_time),
        "asr" to PrayerSlotViews(R.id.iv_asr_icon, R.id.tv_asr_name, R.id.tv_asr_time),
        "maghrib" to PrayerSlotViews(R.id.iv_maghrib_icon, R.id.tv_maghrib_name, R.id.tv_maghrib_time),
        "isha" to PrayerSlotViews(R.id.iv_isha_icon, R.id.tv_isha_name, R.id.tv_isha_time)
    )

    // Update prayer times using the shared helper data and functions
    prayerSlotsInfo.forEach { slotInfo ->
        prayerViewMappings[slotInfo.key]?.let { viewIds ->
            val rawTime24h = getPrayerTime(prefs, slotInfo.key)
            val formattedTime12h = formatTime12h(rawTime24h)

            views.setImageViewResource(viewIds.iconViewId, slotInfo.iconResId)
            views.setTextViewText(viewIds.nameViewId, slotInfo.displayName)
            views.setTextViewText(viewIds.timeViewId, formattedTime12h)
        }
    }

    setClickListeners(context, views)

    appWidgetManager.updateAppWidget(appWidgetId, views)
    Log.d("PrayerWidget", "Widget UI and click listeners updated for widgetId: $appWidgetId")
}

private fun setClickListeners(context: Context, views: RemoteViews) {
    setDateContainerClickListener(context, views)
    setAlarmClockClickListener(context, views)
}

private fun setDateContainerClickListener(context: Context, views: RemoteViews) {
    val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
        data = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(System.currentTimeMillis().toString())
            .build()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val calendarPendingIntent = createPendingIntent(context, calendarIntent, requestCode = 0)
    views.setOnClickPendingIntent(R.id.date_container, calendarPendingIntent)
}

private fun setAlarmClockClickListener(context: Context, views: RemoteViews) {
    getLaunchAppPendingIntent(context, "zte.com.cn.alarmclock", requestCode = 1)?.let {
        views.setOnClickPendingIntent(R.id.clock_container, it)
        views.setOnClickPendingIntent(R.id.prayer_times_layout, it)
    }
}

private fun getLaunchAppPendingIntent(context: Context, packageName: String, requestCode: Int): PendingIntent? {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return null
    return createPendingIntent(context, launchIntent, requestCode)
}

private fun createPendingIntent(context: Context, intent: Intent, requestCode: Int): PendingIntent {
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    return PendingIntent.getActivity(context, requestCode, intent, flags)
}