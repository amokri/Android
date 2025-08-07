package com.ahm.mytime

import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.ahm.mytime.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val DAILY_PRAYER_TIME_WORK_NAME = "DailyPrayerTimeFetchWork"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        scheduleOneTimePrayerTimeWorker()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun setupClickListeners() {
        // Refresh button listener
        binding.refreshButton.setOnClickListener {
            Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show()
            scheduleOneTimePrayerTimeWorker()
        }

        // Calendar click listener
        binding.clock.dateContainer.setOnClickListener {
            val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI.buildUpon()
                    .appendPath("time")
                    .appendPath(System.currentTimeMillis().toString())
                    .build()
            }
            try {
                startActivity(calendarIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open Calendar app", Toast.LENGTH_SHORT).show()
            }
        }

        // Alarm clock click listener
        val alarmClockListener = View.OnClickListener {
            val packageName = "zte.com.cn.alarmclock" // As defined in the widget
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                try {
                    startActivity(launchIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open Alarm Clock app", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Alarm Clock app not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.clock.clockContainer.setOnClickListener(alarmClockListener)
        binding.clock.prayerTimesLayout.setOnClickListener(alarmClockListener)
    }

    private fun updateUI() {
        binding.statusText.visibility = View.VISIBLE

        // Update date info
        binding.clock.tvDay.text = getTodayDayName()
        binding.clock.tvDate.text = getTodayGregorianDate()

        val prefs = getPrefs(this)
        binding.clock.tvIslamicDateLocation.text = getTodayHijriDate(prefs)

        // Update prayer times
        val prayerViewMap = mapOf(
            "fajr" to Triple(binding.clock.ivFajrIcon, binding.clock.tvFajrName, binding.clock.tvFajrTime),
            "sunrise" to Triple(binding.clock.ivSunriseIcon, binding.clock.tvSunriseName, binding.clock.tvSunriseTime),
            "dhuhr" to Triple(binding.clock.ivDhuhrIcon, binding.clock.tvDhuhrName, binding.clock.tvDhuhrTime),
            "asr" to Triple(binding.clock.ivAsrIcon, binding.clock.tvAsrName, binding.clock.tvAsrTime),
            "maghrib" to Triple(binding.clock.ivMaghribIcon, binding.clock.tvMaghribName, binding.clock.tvMaghribTime),
            "isha" to Triple(binding.clock.ivIshaIcon, binding.clock.tvIshaName, binding.clock.tvIshaTime)
        )

        prayerSlotsInfo.forEach { slotInfo ->
            prayerViewMap[slotInfo.key]?.let { (iconView, nameView, timeView) ->
                val rawTime24h = getPrayerTime(prefs, slotInfo.key)
                iconView.setImageResource(slotInfo.iconResId)
                nameView.text = slotInfo.displayName
                timeView.text = formatTime12h(rawTime24h)
            }
        }

        binding.statusText.visibility = View.GONE
    }

    private fun scheduleOneTimePrayerTimeWorker() {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<PrayerTimesFetchWorker>()
            .setConstraints(createNetworkConstraints())
            .addTag(DAILY_PRAYER_TIME_WORK_NAME)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            DAILY_PRAYER_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }
}