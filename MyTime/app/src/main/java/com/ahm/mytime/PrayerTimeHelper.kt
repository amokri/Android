package com.ahm.mytime

import android.content.Context
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import java.util.*

// Shared Formatting
internal val KEY_DATE_FORMAT = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
private val INPUT_TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.US)
private val OUTPUT_TIME_FORMAT = SimpleDateFormat("h:mm a", Locale.US)

// Shared Data
data class PrayerSlotInfo(val key: String, val displayName: String, val iconResId: Int)

internal val prayerSlotsInfo = listOf(
    PrayerSlotInfo("fajr", "Fajr", R.drawable.ic_fajr),
    PrayerSlotInfo("sunrise", "Sunrise", R.drawable.ic_sunrise),
    PrayerSlotInfo("dhuhr", "Dhuhr", R.drawable.ic_dhuhr),
    PrayerSlotInfo("asr", "Asr", R.drawable.ic_asr),
    PrayerSlotInfo("maghrib", "Maghrib", R.drawable.ic_maghrib),
    PrayerSlotInfo("isha", "Isha", R.drawable.ic_isha)
)

// Shared Logic
internal fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PrayerTimesFetchWorker.PRAYER_PREFS, Context.MODE_PRIVATE)
}

internal fun formatTime12h(timeString: String?): String {
    return try {
        val date = INPUT_TIME_FORMAT.parse(timeString ?: "--:--")
        OUTPUT_TIME_FORMAT.format(date ?: Date())
    } catch (e: Exception) {
        timeString ?: "--:--"
    }
}

internal fun getTodayDayName(): String = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

internal fun getTodayGregorianDate(): String = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date())

internal fun getTodayHijriDate(prefs: SharedPreferences): String {
    val todayDateKey = KEY_DATE_FORMAT.format(Date())
    val dynamicKey = "islamic_date_$todayDateKey"
    return prefs.getString(dynamicKey, "...") ?: "..."
}

internal fun getPrayerTime(prefs: SharedPreferences, prayerKey: String): String {
    val todayDateKey = KEY_DATE_FORMAT.format(Date())
    val dynamicKey = "${prayerKey}_$todayDateKey"
    return prefs.getString(dynamicKey, "--:--") ?: "--:--"
}