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
            prayerTimesSuccess = fetchAndSavePrayerTimes(prefsEditor)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching prayer times", e)
        }

        try {
            hijriDateSuccess = fetchAndSaveMonthlyHijriDates(prefsEditor)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Hijri dates", e)
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
                    val date = cells[0].select("h6").text()

                    if (date.isNotBlank()) {
                        val fajr = cells[2].text()
                        val sunrise = cells[3].text()
                        val dhuhr = cells[4].text()
                        val asr = cells[5].text()
                        val maghrib = cells[6].text()
                        val isha = cells[7].text()

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

    private fun fetchAndSaveMonthlyHijriDates(editor: SharedPreferences.Editor): Boolean {
        val hijriUrl = "https://timesprayer.com/en/hijri-date-in-malaysia.html"
        Log.d(TAG, "Fetching monthly Hijri dates from: $hijriUrl")

        try {
            val doc = Jsoup.connect(hijriUrl).get()
            val table = doc.select("div.prayertable.calendarTb table").firstOrNull()
            val rows = table?.select("tbody tr")

            if (rows.isNullOrEmpty()) {
                Log.e(TAG, "Could not find Hijri date table or it has no rows.")
                return false
            }

            val gregorianParseFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH)
            val keyStorageFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

            var rowsProcessed = 0
            rows.forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 2) {
                    val gregorianDateStr = cells[0].text()
                    val hijriDateStr = cells[1].select("strong").text()

                    if (gregorianDateStr.isNotBlank() && hijriDateStr.isNotBlank()) {
                        try {
                            val parsedDate = gregorianParseFormat.parse(gregorianDateStr)
                            if (parsedDate != null) {
                                val dateKey = keyStorageFormat.format(parsedDate)
                                editor.putString("islamic_date_$dateKey", hijriDateStr)
                                rowsProcessed++
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not parse date: $gregorianDateStr", e)
                        }
                    }
                }
            }

            return if (rowsProcessed > 0) {
                Log.d(TAG, "Successfully parsed and stored Hijri dates for $rowsProcessed days.")
                true
            } else {
                Log.e(TAG, "No valid Hijri date rows were processed.")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while fetching or parsing monthly Hijri dates", e)
            return false
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