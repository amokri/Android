package com.ahm.mytime

import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.ahm.mytime.databinding.ActivityMainBinding
import java.util.*

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
        scheduleDailyPrayerTimeWorker()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun setupClickListeners() {
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
        updateDateInformation()
        updateHijriDate()
        updatePrayerTimes()
        binding.statusText.visibility = View.GONE
    }

    private fun updateDateInformation() {
        val today = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        binding.clock.tvDay.text = today

        val gregorianDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date())
        binding.clock.tvDate.text = gregorianDate
    }

    private fun updateHijriDate() {
        val prefs = getSharedPreferences(PrayerTimesFetchWorker.PRAYER_PREFS, Context.MODE_PRIVATE)
        val todayDateKey = KEY_DATE_FORMAT.format(Date())
        val dynamicKey = "islamic_date_$todayDateKey"
        val fetchedHijriDate = prefs.getString(dynamicKey, "...")
        binding.clock.tvIslamicDateLocation.text = fetchedHijriDate
    }

    private fun updatePrayerTimes() {
        val prefs = getSharedPreferences(PrayerTimesFetchWorker.PRAYER_PREFS, Context.MODE_PRIVATE)
        val todayDateKey = KEY_DATE_FORMAT.format(Date())

        val prayerSlots = listOf(
            PrayerSlot("fajr", "Fajr", R.drawable.ic_fajr, R.id.iv_fajr_icon, R.id.tv_fajr_name, R.id.tv_fajr_time),
            PrayerSlot("sunrise", "Sunrise", R.drawable.ic_sunrise, R.id.iv_sunrise_icon, R.id.tv_sunrise_name, R.id.tv_sunrise_time),
            PrayerSlot("dhuhr", "Dhuhr", R.drawable.ic_dhuhr, R.id.iv_dhuhr_icon, R.id.tv_dhuhr_name, R.id.tv_dhuhr_time),
            PrayerSlot("asr", "Asr", R.drawable.ic_asr, R.id.iv_asr_icon, R.id.tv_asr_name, R.id.tv_asr_time),
            PrayerSlot("maghrib", "Maghrib", R.drawable.ic_maghrib, R.id.iv_maghrib_icon, R.id.tv_maghrib_name, R.id.tv_maghrib_time),
            PrayerSlot("isha", "Isha", R.drawable.ic_isha, R.id.iv_isha_icon, R.id.tv_isha_name, R.id.tv_isha_time)
        )

        prayerSlots.forEach { slot ->
            val dynamicKey = "${slot.key}_$todayDateKey"
            val rawTime24h = prefs.getString(dynamicKey, "--:--")
            val formattedTime12h = formatTime(rawTime24h, INPUT_TIME_FORMAT, OUTPUT_TIME_FORMAT)

            val (iconView, nameView, timeView) = when (slot.key) {
                "fajr" -> Triple(binding.clock.ivFajrIcon, binding.clock.tvFajrName, binding.clock.tvFajrTime)
                "sunrise" -> Triple(binding.clock.ivSunriseIcon, binding.clock.tvSunriseName, binding.clock.tvSunriseTime)
                "dhuhr" -> Triple(binding.clock.ivDhuhrIcon, binding.clock.tvDhuhrName, binding.clock.tvDhuhrTime)
                "asr" -> Triple(binding.clock.ivAsrIcon, binding.clock.tvAsrName, binding.clock.tvAsrTime)
                "maghrib" -> Triple(binding.clock.ivMaghribIcon, binding.clock.tvMaghribName, binding.clock.tvMaghribTime)
                "isha" -> Triple(binding.clock.ivIshaIcon, binding.clock.tvIshaName, binding.clock.tvIshaTime)
                else -> Triple(null, null, null)
            }

            iconView?.setImageResource(slot.iconResId)
            nameView?.text = slot.displayName
            timeView?.text = formattedTime12h
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

    private fun scheduleDailyPrayerTimeWorker() {
        val constraints = createNetworkConstraints()
        scheduleOneTimePrayerTimeWork(constraints)
    }

    private fun createNetworkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    private fun scheduleOneTimePrayerTimeWork(constraints: Constraints) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<PrayerTimesFetchWorker>()
            .setConstraints(constraints)
            .addTag(DAILY_PRAYER_TIME_WORK_NAME)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            DAILY_PRAYER_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }
}