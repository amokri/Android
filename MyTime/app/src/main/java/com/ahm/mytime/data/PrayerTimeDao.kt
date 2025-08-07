package com.ahm.mytime.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prayerTimes: List<PrayerTime>)

    @Query("SELECT * FROM prayer_times WHERE date = :date LIMIT 1")
    suspend fun getPrayerTimeForDate(date: String): PrayerTime?

    @Query("SELECT * FROM prayer_times ORDER BY date ASC")
    fun getAllPrayerTimes(): Flow<List<PrayerTime>>

    @Query("DELETE FROM prayer_times")
    suspend fun clearAll()
}