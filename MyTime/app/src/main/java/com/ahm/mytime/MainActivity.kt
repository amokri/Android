package com.ahm.mytime

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val DAILY_PRAYER_TIME_WORK_NAME = "DailyPrayerTimeFetchWork"
    }

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scheduleDailyPrayerTimeWorker()
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
