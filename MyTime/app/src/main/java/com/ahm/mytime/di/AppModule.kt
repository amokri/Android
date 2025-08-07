package com.ahm.mytime.di

import android.content.Context
import androidx.room.Room
import com.ahm.mytime.data.PrayerTimeDao
import com.ahm.mytime.data.PrayerTimeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePrayerTimeDatabase(@ApplicationContext context: Context): PrayerTimeDatabase {
        return Room.databaseBuilder(
            context,
            PrayerTimeDatabase::class.java,
            "prayer_time_database"
        ).build()
    }

    @Provides
    fun providePrayerTimeDao(database: PrayerTimeDatabase): PrayerTimeDao {
        return database.prayerTimeDao()
    }
}