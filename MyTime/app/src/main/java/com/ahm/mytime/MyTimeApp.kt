package com.ahm.mytime

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyTimeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        schedulePrayerTimeFetcher()
    }

    private fun schedulePrayerTimeFetcher() {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<PrayerTimesFetchWorker>(1, TimeUnit.DAYS)
            .setConstraints(createNetworkConstraints())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MonthlyPrayerTimeFetchWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        )
    }
}