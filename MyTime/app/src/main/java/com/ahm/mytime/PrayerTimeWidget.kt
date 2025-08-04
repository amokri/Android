package com.ahm.mytime

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
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
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

// Data class for prayer slots (remains the same)
data class PrayerSlot(
    val key: String,
    val displayName: String,
    val iconResId: Int,
    val iconViewId: Int,
    val nameViewId: Int,
    val timeViewId: Int
)

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.widget_prayer_time)
    val prefs = context.getSharedPreferences(PrayerTimesFetchWorker.PRAYER_PREFS, Context.MODE_PRIVATE)

    updateDateInformation(views)
    updateHijriDate(views, prefs)
    updatePrayerTimes(views, prefs)

    setClickListeners(context, views)

    appWidgetManager.updateAppWidget(appWidgetId, views)
    Log.d("PrayerWidget", "Widget UI and click listeners updated for widgetId: $appWidgetId")
}

private fun updateDateInformation(views: RemoteViews) {
    val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    views.setTextViewText(R.id.tv_day, today)

    val gregorianDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date())
    views.setTextViewText(R.id.tv_date, gregorianDate)
}

private fun updateHijriDate(views: RemoteViews, prefs: android.content.SharedPreferences) {
    val fetchedHijriDate = prefs.getString("islamic_date", "...")
    val islamicDateStr = "$fetchedHijriDate"
    views.setTextViewText(R.id.tv_islamic_date_location, islamicDateStr)
}

private fun updatePrayerTimes(views: RemoteViews, prefs: android.content.SharedPreferences) {
    val inputFormat = SimpleDateFormat("HH:mm", Locale.US)
    val outputFormat = SimpleDateFormat("h:mm a", Locale.US)

    // Get the current date in the format "dd-MM-yyyy" to match the keys in SharedPreferences
    val keyDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
    val todayDateKey = keyDateFormat.format(Date())

    val prayerSlots = listOf(
        PrayerSlot("fajr", "Fajr", R.drawable.ic_fajr, R.id.iv_fajr_icon, R.id.tv_fajr_name, R.id.tv_fajr_time),
        PrayerSlot("sunrise", "Sunrise", R.drawable.ic_sunrise, R.id.iv_sunrise_icon, R.id.tv_sunrise_name, R.id.tv_sunrise_time),
        PrayerSlot("dhuhr", "Dhuhr", R.drawable.ic_dhuhr, R.id.iv_dhuhr_icon, R.id.tv_dhuhr_name, R.id.tv_dhuhr_time),
        PrayerSlot("asr", "Asr", R.drawable.ic_asr, R.id.iv_asr_icon, R.id.tv_asr_name, R.id.tv_asr_time),
        PrayerSlot("maghrib", "Maghrib", R.drawable.ic_maghrib, R.id.iv_maghrib_icon, R.id.tv_maghrib_name, R.id.tv_maghrib_time),
        PrayerSlot("isha", "Isha", R.drawable.ic_isha, R.id.iv_isha_icon, R.id.tv_isha_name, R.id.tv_isha_time)
    )

    prayerSlots.forEach { slot ->
        // Construct the dynamic key using the prayer name and today's date
        val dynamicKey = "${slot.key}_$todayDateKey"
        val rawTime24h = prefs.getString(dynamicKey, "--:--")
        val formattedTime12h = formatTime(rawTime24h, inputFormat, outputFormat)

        views.setImageViewResource(slot.iconViewId, slot.iconResId)
        views.setTextViewText(slot.nameViewId, slot.displayName)
        views.setTextViewText(slot.timeViewId, formattedTime12h)
    }
}

private fun formatTime(
    timeString: String?,
    inputFormat: SimpleDateFormat,
    outputFormat: SimpleDateFormat
): String {
    return try {
        val date = inputFormat.parse(timeString ?: "--:--")
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        timeString ?: "--:--"
    }
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

private fun createPendingIntent(context: Context, intent: Intent, requestCode: Int): PendingIntent {
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    return PendingIntent.getActivity(context, requestCode, intent, flags)
}

private fun getLaunchAppPendingIntent(context: Context, packageName: String, requestCode: Int): PendingIntent? {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return null
    return createPendingIntent(context, launchIntent, requestCode)
}