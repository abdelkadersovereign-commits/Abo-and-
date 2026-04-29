package com.asyria.security.ui.screens

import android.app.Application
import androidx.lifecycle.*
import com.asyria.security.data.prayer.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

data class PrayerUiState(
    val timings: Timings? = null,
    val nextPrayerName: String = "",
    val nextPrayerTime: String = "",
    val countdown: String = "00:00:00",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isHubOpen: Boolean = false
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val db = AppDatabase.getDatabase(application)
    private val api = Retrofit.Builder()
        .baseUrl(AladhanApi.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AladhanApi::class.java)

    private var timer: Timer? = null

    init {
        fetchPrayerTimes()
        startCountdownTimer()
    }

    fun setHubOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isHubOpen = open)
    }

    fun fetchPrayerTimes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // Try local first
            val localTimes = db.prayerDao().getPrayerTimesForDate(today)
            if (localTimes != null) {
                _uiState.value = _uiState.value.copy(
                    timings = Timings(localTimes.fajr, localTimes.dhuhr, localTimes.asr, localTimes.maghrib, localTimes.isha),
                    isLoading = false
                )
                updateNextPrayer()
            } else {
                // Fetch from API (defaulting to Damascus for this demo, or we could use location)
                try {
                    val response = api.getTimings("Damascus", "Syria")
                    val timings = response.data.timings
                    
                    // Cache it
                    db.prayerDao().insertPrayerTimes(
                        PrayerTimesEntity(
                            date = today,
                            fajr = timings.Fajr,
                            dhuhr = timings.Dhuhr,
                            asr = timings.Asr,
                            maghrib = timings.Maghrib,
                            isha = timings.Isha
                        )
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        timings = timings,
                        isLoading = false
                    )
                    updateNextPrayer()
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to sync spiritual timing fragments."
                    )
                }
            }
        }
    }

    private fun startCountdownTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateNextPrayer()
            }
        }, 0, 1000)
    }

    private fun updateNextPrayer() {
        val timings = _uiState.value.timings ?: return
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val prayers = listOf(
            "Fajr" to timings.Fajr,
            "Dhuhr" to timings.Dhuhr,
            "Asr" to timings.Asr,
            "Maghrib" to timings.Maghrib,
            "Isha" to timings.Isha
        )

        var nextItem: Pair<String, String>? = null
        var nextTimeCal: Calendar? = null

        for (prayer in prayers) {
            val prayerTime = sdf.parse(prayer.second.substringBefore(" ")) ?: continue
            val cal = Calendar.getInstance().apply {
                time = prayerTime
                set(Calendar.YEAR, now.get(Calendar.YEAR))
                set(Calendar.MONTH, now.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            }
            
            if (cal.after(now)) {
                nextItem = prayer
                nextTimeCal = cal
                break
            }
        }

        // If all prayers passed, next is Fajr tomorrow
        if (nextItem == null) {
            nextItem = "Fajr" to timings.Fajr
            val prayerTime = sdf.parse(timings.Fajr.substringBefore(" "))!!
            nextTimeCal = Calendar.getInstance().apply {
                time = prayerTime
                set(Calendar.YEAR, now.get(Calendar.YEAR))
                set(Calendar.MONTH, now.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val diff = nextTimeCal!!.timeInMillis - now.timeInMillis
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        val seconds = (diff / 1000) % 60
        
        val countdown = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        _uiState.value = _uiState.value.copy(
            nextPrayerName = nextItem.first,
            nextPrayerTime = nextItem.second,
            countdown = countdown
        )
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }
}
