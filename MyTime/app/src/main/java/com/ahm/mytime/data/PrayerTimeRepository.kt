package com.ahm.mytime.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimeRepository @Inject constructor(private val prayerTimeDao: PrayerTimeDao) {
    suspend fun getPrayerTimeForDate(date: String): PrayerTime? = prayerTimeDao.getPrayerTimeForDate(date)
    suspend fun insertPrayerTimes(prayerTimes: List<PrayerTime>) {
        prayerTimeDao.insertAll(prayerTimes)
    }
}