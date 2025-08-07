package com.ahm.mytime

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object MidnightAlarmScheduler {

    private const val ALARM_REQUEST_CODE = 101

    fun scheduleMidnightUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w("MidnightAlarmScheduler", "Cannot schedule exact alarms. Prompting user.")
            // Guide user to settings to grant permission
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                context.startActivity(it)
            }
            return
        }

        val intent = Intent(context, PrayerTimeWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, PrayerTimeWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate next midnight
        val nextMidnight = ZonedDateTime.of(
            LocalDate.now(ZoneId.systemDefault()).plusDays(1),
            LocalTime.MIDNIGHT,
            ZoneId.systemDefault()
        )

        // Use setExactAndAllowWhileIdle for reliability
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnight.toInstant().toEpochMilli(),
            pendingIntent
        )

        Log.d("MidnightAlarmScheduler", "Midnight widget update alarm scheduled for ${nextMidnight}.")
    }
}