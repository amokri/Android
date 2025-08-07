package com.ahm.mytime.di

import com.ahm.mytime.data.PrayerTimeRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositoryEntryPoint {
    fun prayerTimeRepository(): PrayerTimeRepository
}