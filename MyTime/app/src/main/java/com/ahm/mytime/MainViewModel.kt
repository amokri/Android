package com.ahm.mytime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahm.mytime.data.PrayerTime
import com.ahm.mytime.data.PrayerTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: PrayerTimeRepository
) : ViewModel() {

    private val _prayerTime = MutableStateFlow<PrayerTime?>(null)
    val prayerTime: StateFlow<PrayerTime?> = _prayerTime.asStateFlow()

    fun loadPrayerTimeForToday() {
        viewModelScope.launch {
            val todayDateKey = KEY_DATE_FORMAT.format(Date())
            _prayerTime.value = repository.getPrayerTimeForDate(todayDateKey)
        }
    }
}