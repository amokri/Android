package com.ahm.mytime.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PrayerTime::class], version = 1, exportSchema = false)
abstract class PrayerTimeDatabase : RoomDatabase() {
    abstract fun prayerTimeDao(): PrayerTimeDao
}