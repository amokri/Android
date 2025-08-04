package com.ahm.mytime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PRAYER_TIME_WORK_NAME = "PrayerTimeFetchWork"
        private const val INITIAL_PRAYER_TIME_WORK_NAME = "InitialPrayerTimeFetch"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        schedulePrayerTimeWorker()
    }

    private fun schedulePrayerTimeWorker() {
        val constraints = createNetworkConstraints()

        schedulePeriodicPrayerTimeWork(constraints)
        scheduleInitialPrayerTimeWork(constraints)
    }

    private fun createNetworkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    private fun schedulePeriodicPrayerTimeWork(constraints: Constraints) {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<PrayerTimesFetchWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PRAYER_TIME_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    private fun scheduleInitialPrayerTimeWork(constraints: Constraints) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<PrayerTimesFetchWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            INITIAL_PRAYER_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            oneTimeWorkRequest
        )
    }
}