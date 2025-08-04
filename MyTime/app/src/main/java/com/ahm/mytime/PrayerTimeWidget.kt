package com.ahm.mytime

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.provider.CalendarContract
import android.text.Html
import android.util.Log
import android.widget.RemoteViews
import java.util.*

private val KEY_DATE_FORMAT = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
private val INPUT_TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.US)
private val OUTPUT_TIME_FORMAT = SimpleDateFormat("h:mm a", Locale.US)

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
    updateHijriDate(context, views, prefs)
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

private fun updateHijriDate(context: Context, views: RemoteViews, prefs: SharedPreferences) {
    val todayDateKey = KEY_DATE_FORMAT.format(Date())
    val dynamicKey = "islamic_date_$todayDateKey"

    val fetchedHijriDate = prefs.getString(dynamicKey, "...")
    views.setTextViewText(R.id.tv_islamic_date_location, fetchedHijriDate)
}

private fun updatePrayerTimes(views: RemoteViews, prefs: SharedPreferences) {
    val todayDateKey = KEY_DATE_FORMAT.format(Date())

    val prayerSlots = listOf(
        PrayerSlot("fajr", "Fajr", R.drawable.ic_fajr, R.id.iv_fajr_icon, R.id.tv_fajr_name, R.id.tv_fajr_time),
        PrayerSlot("sunrise", "Sunrise", R.drawable.ic_sunrise, R.id.iv_sunrise_icon, R.id.tv_sunrise_name, R.id.tv_sunrise_time),
        PrayerSlot("dhuhr", "Dhuhr", R.drawable.ic_dhuhr, R.id.iv_dhuhr_icon, R.id.tv_dhuhr_name, R.id.tv_dhuhr_time),
        PrayerSlot("asr", "Asr", R.drawable.ic_asr, R.id.iv_asr_icon, R.id.tv_asr_name, R.id.tv_asr_time),
        PrayerSlot("maghrib", "Maghrib", R.drawable.ic_maghrib, R.id.iv_maghrib_icon, R.id.tv_maghrib_name, R.id.tv_maghrib_time),
        PrayerSlot("isha", "Isha", R.drawable.ic_isha, R.id.iv_isha_icon, R.id.tv_isha_name, R.id.tv_isha_time)
    )

    val prayerKeys = prayerSlots.map { it.key }
    val currentPrayerIndex = getCurrentPrayerIndex(prefs, prayerKeys)

    prayerSlots.forEachIndexed { index, slot ->
        val dynamicKey = "${slot.key}_$todayDateKey"
        val rawTime24h = prefs.getString(dynamicKey, "--:--")
        val formattedTime12h = formatTime(rawTime24h, INPUT_TIME_FORMAT, OUTPUT_TIME_FORMAT)

        views.setImageViewResource(slot.iconViewId, slot.iconResId)

        if (index == currentPrayerIndex) {
            // Apply bold style to the current prayer time
            views.setTextViewText(slot.nameViewId, getBoldedText(slot.displayName))
            views.setTextViewText(slot.timeViewId, getBoldedText(formattedTime12h))
        } else {
            // Apply normal style to other prayer times
            views.setTextViewText(slot.nameViewId, slot.displayName)
            views.setTextViewText(slot.timeViewId, formattedTime12h)
        }
    }
}

/**
 * Wraps the given text in <b> and <font> HTML tags to make it bold and yellow.
 */
private fun getBoldedText(text: String): CharSequence {
    // A pleasant yellow color. You can change this hex code.
    val yellowColor = "#77EB3B"
    val styledText = "<font color='$yellowColor'><b>$text</b></font>"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(styledText, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(styledText)
    }
}

/**
 * Determines the index of the current prayer by comparing prayer times with the current system time.
 * The "current" prayer is defined as the last prayer whose time has passed.
 * Before Fajr, Isha is considered the current prayer.
 */
private fun getCurrentPrayerIndex(prefs: SharedPreferences, prayerKeys: List<String>): Int {
    val todayDateKey = KEY_DATE_FORMAT.format(Date())
    val now = Calendar.getInstance()

    val prayerCalendars = prayerKeys.map { key ->
        val dynamicKey = "${key}_$todayDateKey"
        val timeString = prefs.getString(dynamicKey, null)
        getCalendarForPrayerTime(timeString)
    }

    var currentPrayerIndex = -1

    // Iterate backwards to find the last prayer that has passed.
    // e.g., if it's 3 PM, the last prayer passed was Dhuhr.
    for (i in prayerCalendars.indices.reversed()) {
        val prayerTime = prayerCalendars[i]
        if (prayerTime != null && !prayerTime.after(now)) {
            currentPrayerIndex = i
            break
        }
    }

    // If no prayer has passed today (e.g., it's before Fajr),
    // the current prayer is considered Isha (the last one in the list).
    if (currentPrayerIndex == -1) {
        return prayerKeys.size - 1
    }

    return currentPrayerIndex
}

/**
 * Converts a time string (e.g., "13:15") into a Calendar object set to today's date.
 */
private fun getCalendarForPrayerTime(timeString: String?): Calendar? {
    if (timeString.isNullOrBlank() || timeString == "--:--") return null
    return try {
        val parsedTime = INPUT_TIME_FORMAT.parse(timeString)
        val prayerCal = Calendar.getInstance()
        if (parsedTime != null) {
            prayerCal.time = parsedTime
        }

        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, prayerCal.get(Calendar.HOUR_OF_DAY))
        todayCal.set(Calendar.MINUTE, prayerCal.get(Calendar.MINUTE))
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        todayCal
    } catch (e: Exception) {
        Log.e("PrayerWidget", "Failed to parse time: $timeString", e)
        null
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
//    val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
//        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//    }
//
//    if (alarmIntent.resolveActivity(context.packageManager) != null) {
//        val alarmPendingIntent = createPendingIntent(context, alarmIntent, requestCode = 1)
//        views.setOnClickPendingIntent(R.id.clock_container, alarmPendingIntent)
//        views.setOnClickPendingIntent(R.id.prayer_times_layout, alarmPendingIntent)
//    } else {
//        Log.w("PrayerWidget", "No application found to handle ACTION_SHOW_ALARMS intent.")
    setZTEAlarmClockClickListener(context, views)
//    }
}

private fun setZTEAlarmClockClickListener(context: Context, views: RemoteViews) {
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