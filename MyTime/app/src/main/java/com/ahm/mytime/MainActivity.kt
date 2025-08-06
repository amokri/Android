package com.ahm.mytime

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import java.util.concurrent.TimeUnit
// Removed unused imports: Activity, AppWidgetManager, Intent, RemoteViews, View

class MainActivity : AppCompatActivity() {

    companion object {
        // Renamed from INITIAL_PRAYER_TIME_WORK_NAME for clarity
        private const val DAILY_PRAYER_TIME_WORK_NAME = "DailyPrayerTimeFetchWork"
        // Removed PRAYER_TIME_WORK_NAME as periodic work is removed
    }

    @SuppressLint(
        "ResourceType" // This annotation might be related to R.layout.activity_main or other resources
    )
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

    // Renamed from scheduleInitialPrayerTimeWork
    private fun scheduleOneTimePrayerTimeWork(constraints: Constraints) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<PrayerTimesFetchWorker>()
            .setConstraints(constraints)
            // It's good practice to add a tag for cancellability or observation
            .addTag(DAILY_PRAYER_TIME_WORK_NAME)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            DAILY_PRAYER_TIME_WORK_NAME, // Use the new constant for unique work name
            ExistingWorkPolicy.REPLACE, // Ensures work runs on app launch, replacing any existing scheduled work
            oneTimeWorkRequest
        )
    }
}
