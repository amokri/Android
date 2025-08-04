package com.ahm.mytime

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.jsoup.Jsoup
import java.util.*

class PrayerTimesFetchWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val PRAYER_PREFS = "PrayerTimePrefs"
        private const val TAG = "PrayerTimesFetchWorker"
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PRAYER_PREFS, Context.MODE_PRIVATE).edit()
        var prayerTimesSuccess = false
        var hijriDateSuccess = false

        try {
            prayerTimesSuccess = fetchAndSavePrayerTimes(prefs)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching prayer times", e)
        }

        try {
            hijriDateSuccess = fetchAndSaveHijriDate(prefs)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Hijri date", e)
        }

        prefs.apply()

        return if (prayerTimesSuccess && hijriDateSuccess) {
            triggerWidgetUpdate()
            Result.success()
        } else {
            Log.e(TAG, "One or more fetch tasks failed. PrayerSuccess: $prayerTimesSuccess, HijriSuccess: $hijriDateSuccess")
            Result.failure()
        }
    }

    private fun fetchAndSavePrayerTimes(editor: android.content.SharedPreferences.Editor): Boolean {
        val prayerUrl = "https://www.waktusolat.my/kedah/kdh05"
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
        val dateToFind = dateFormat.format(Date())

        Log.d(TAG, "Fetching prayer times for: $dateToFind from $prayerUrl")

        val prayerDoc = Jsoup.connect(prayerUrl).get()
        val table = prayerDoc.select("table#waktu-semua").firstOrNull()
        val row = table?.select("tbody tr")?.find {
            it.select("td:first-child h6").text() == dateToFind
        }

        return if (row != null) {
            val cells = row.select("td")
            if (cells.size >= 8) {
                editor.putString("fajr", cells[2].text())
                editor.putString("sunrise", cells[3].text())
                editor.putString("dhuhr", cells[4].text())
                editor.putString("asr", cells[5].text())
                editor.putString("maghrib", cells[6].text())
                editor.putString("isha", cells[7].text())
                Log.d(TAG, "Successfully parsed prayer times.")
                true
            } else {
                Log.e(TAG, "Insufficient data in prayer time row.")
                false
            }
        } else {
            Log.e(TAG, "Could not find prayer time row for today.")
            false
        }
    }

    private fun fetchAndSaveHijriDate(editor: android.content.SharedPreferences.Editor): Boolean {
        val hijriUrl = "https://timesprayer.com/en/hijri-date-in-malaysia.html"
        Log.d(TAG, "Fetching Hijri date from: $hijriUrl")

        val hijriDoc = Jsoup.connect(hijriUrl).get()
        val hijriDateElement = hijriDoc.select("td[itemprop=text] strong").first()

        return if (hijriDateElement != null) {
            val rawHijriDate = hijriDateElement.text()
            val cleanedHijriDate = rawHijriDate.replace(" Hijri", "").trim()
            editor.putString("islamic_date", cleanedHijriDate)
            Log.d(TAG, "Successfully parsed Hijri date: $cleanedHijriDate")
            true
        } else {
            Log.e(TAG, "Could not find Hijri date element.")
            false
        }
    }

    private fun triggerWidgetUpdate() {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val componentName = ComponentName(
            applicationContext.packageName,
            PrayerTimeWidget::class.java.name
        )
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent(applicationContext, PrayerTimeWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            applicationContext.sendBroadcast(intent)
            Log.d(TAG, "Broadcast sent to update widget.")
        }
    }
}