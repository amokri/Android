package com.ahm.mytime

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
        val prefsEditor = applicationContext.getSharedPreferences(PRAYER_PREFS, Context.MODE_PRIVATE).edit()
        var prayerTimesSuccess = false
        var hijriDateSuccess = false

        try {
            // Clear old prayer times before fetching new ones to prevent stale data.
            // A more sophisticated approach might be needed if other data is stored,
            // but for this scope, clearing all is acceptable.
            // prefsEditor.clear()
            prayerTimesSuccess = fetchAndSavePrayerTimes(prefsEditor)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching prayer times", e)
        }

        try {
            hijriDateSuccess = fetchAndSaveHijriDate(prefsEditor)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Hijri date", e)
        }

        prefsEditor.apply()

        return if (prayerTimesSuccess && hijriDateSuccess) {
            triggerWidgetUpdate()
            Result.success()
        } else {
            Log.e(TAG, "One or more fetch tasks failed. PrayerSuccess: $prayerTimesSuccess, HijriSuccess: $hijriDateSuccess")
            Result.failure()
        }
    }

    private fun fetchAndSavePrayerTimes(editor: SharedPreferences.Editor): Boolean {
        val prayerUrl = "https://www.waktusolat.my/kedah/kdh05"
        Log.d(TAG, "Fetching monthly prayer times from: $prayerUrl")

        try {
            val prayerDoc = Jsoup.connect(prayerUrl).get()
            val table = prayerDoc.select("table#waktu-semua").firstOrNull()
            val rows = table?.select("tbody tr")

            if (rows.isNullOrEmpty()) {
                Log.e(TAG, "Could not find prayer time table or it has no rows.")
                return false
            }

            var rowsProcessed = 0
            rows.forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 8) {
                    // The date in the first column is in "dd-MM-yyyy" format
                    val date = cells[0].select("h6").text()

                    if (date.isNotBlank()) {
                        val fajr = cells[2].text()
                        val sunrise = cells[3].text()
                        val dhuhr = cells[4].text()
                        val asr = cells[5].text()
                        val maghrib = cells[6].text()
                        val isha = cells[7].text()

                        // Store each prayer time with a unique key: "prayerName_dd-MM-yyyy"
                        editor.putString("fajr_$date", fajr)
                        editor.putString("sunrise_$date", sunrise)
                        editor.putString("dhuhr_$date", dhuhr)
                        editor.putString("asr_$date", asr)
                        editor.putString("maghrib_$date", maghrib)
                        editor.putString("isha_$date", isha)
                        rowsProcessed++
                    }
                }
            }

            return if (rowsProcessed > 0) {
                Log.d(TAG, "Successfully parsed and stored prayer times for $rowsProcessed days.")
                true
            } else {
                Log.e(TAG, "No valid prayer time rows were processed.")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching or parsing monthly prayer times", e)
            return false
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