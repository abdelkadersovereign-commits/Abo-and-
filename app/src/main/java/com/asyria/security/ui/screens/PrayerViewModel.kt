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

import androidx.work.*
import com.asyria.security.R
import com.asyria.security.worker.NotificationWorker
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class PrayerUiState(
    val timings: Timings? = null,
    val nextPrayerName: String = "",
    val nextPrayerTime: String = "",
    val countdown: String = "00:00:00",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isHubOpen: Boolean = false,
    val supplications: List<SupplicationEntity> = emptyList(),
    val city: String
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PrayerUiState(city = application.getString(R.string.detecting_sovereignty)))
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val db = AppDatabase.getDatabase(application)
    private val workManager = WorkManager.getInstance(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    
    private val api = Retrofit.Builder()
        .baseUrl(AladhanApi.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AladhanApi::class.java)

    private var timer: Timer? = null

    init {
        initSpiritualCore()
        loadSupplications()
        startCountdownTimer()
    }

    private fun initSpiritualCore() {
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            fetchPrayerTimes(location.latitude, location.longitude)
                        } else {
                            fetchPrayerTimes() // Fallback to default
                        }
                    }
                    .addOnFailureListener { fetchPrayerTimes() }
            } else {
                fetchPrayerTimes()
            }
        }
    }

    private fun loadSupplications() {
        viewModelScope.launch {
            val list = db.supplicationDao().getAllSupplications()
            if (list.isEmpty()) {
                val seed = listOf(
                    SupplicationEntity(
                        category = getApplication<Application>().getString(R.string.supplication_category_morning),
                        content = getApplication<Application>().getString(R.string.morning_supplication_1_content),
                        translation = getApplication<Application>().getString(R.string.morning_supplication_1_translation),
                        resonance = getApplication<Application>().getString(R.string.morning_supplication_1_resonance)
                    ),
                    SupplicationEntity(
                        category = getApplication<Application>().getString(R.string.supplication_category_evening),
                        content = getApplication<Application>().getString(R.string.evening_supplication_1_content),
                        translation = getApplication<Application>().getString(R.string.evening_supplication_1_translation),
                        resonance = getApplication<Application>().getString(R.string.evening_supplication_1_resonance)
                    ),
                    SupplicationEntity(
                        category = getApplication<Application>().getString(R.string.supplication_category_soul_calming),
                        content = getApplication<Application>().getString(R.string.soul_calming_supplication_1_content),
                        translation = getApplication<Application>().getString(R.string.soul_calming_supplication_1_translation),
                        resonance = getApplication<Application>().getString(R.string.soul_calming_supplication_1_resonance)
                    ),
                    SupplicationEntity(
                        category = getApplication<Application>().getString(R.string.supplication_category_misc),
                        content = getApplication<Application>().getString(R.string.misc_supplication_1_content),
                        translation = getApplication<Application>().getString(R.string.misc_supplication_1_translation),
                        resonance = getApplication<Application>().getString(R.string.misc_supplication_1_resonance)
                    )
                )
                db.supplicationDao().insertSupplications(seed)
                _uiState.value = _uiState.value.copy(supplications = seed)
            } else {
                _uiState.value = _uiState.value.copy(supplications = list)
            }
        }
    }

    fun setHubOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isHubOpen = open)
    }

    fun fetchPrayerTimes(lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            val localTimes = db.prayerDao().getPrayerTimesForDate(today)
            if (localTimes != null) {
                _uiState.value = _uiState.value.copy(
                    timings = Timings(localTimes.fajr, localTimes.dhuhr, localTimes.asr, localTimes.maghrib, localTimes.isha),
                    isLoading = false,
                    city = getApplication<Application>().getString(R.string.local_sanctuary_cache)
                )
                updateNextPrayer()
                scheduleNotifications()
                return@launch
            }

            try {
                val timings: Timings
                val city: String
                if (lat != null && lon != null) {
                    val response = api.getTimingsByLocation(lat, lon)
                    timings = response.data.timings
                    city = response.data.meta.timezone
                } else {
                    val response = api.getTimings(getApplication<Application>().getString(R.string.damascus), getApplication<Application>().getString(R.string.syria))
                    timings = response.data.timings
                    city = getApplication<Application>().getString(R.string.default_city)
                }
                
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
                    city = city,
                    isLoading = false
                )
                updateNextPrayer()
                scheduleNotifications()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = getApplication<Application>().getString(R.string.error_sanctuary_sync)
                )
            }
        }
    }

    private fun scheduleNotifications() {
        val timings = _uiState.value.timings ?: return
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()

        val prayers = listOf(
            getApplication<Application>().getString(R.string.prayer_fajr) to timings.Fajr,
            getApplication<Application>().getString(R.string.prayer_dhuhr) to timings.Dhuhr,
            getApplication<Application>().getString(R.string.prayer_asr) to timings.Asr,
            getApplication<Application>().getString(R.string.prayer_maghrib) to timings.Maghrib,
            getApplication<Application>().getString(R.string.prayer_isha) to timings.Isha
        )

        workManager.cancelAllWorkByTag("prayer_sync")

        for (prayer in prayers) {
            val prayerTime = sdf.parse(prayer.second.substringBefore(" ")) ?: continue
            val prayerCal = Calendar.getInstance().apply {
                time = prayerTime
                set(Calendar.YEAR, now.get(Calendar.YEAR))
                set(Calendar.MONTH, now.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            }

            if (prayerCal.after(now)) {
                val delay = prayerCal.timeInMillis - now.timeInMillis
                val data = workDataOf("prayer_name" to prayer.first)
                val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("prayer_sync")
                    .build()
                workManager.enqueue(request)
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
            getApplication<Application>().getString(R.string.prayer_fajr) to timings.Fajr,
            getApplication<Application>().getString(R.string.prayer_dhuhr) to timings.Dhuhr,
            getApplication<Application>().getString(R.string.prayer_asr) to timings.Asr,
            getApplication<Application>().getString(R.string.prayer_maghrib) to timings.Maghrib,
            getApplication<Application>().getString(R.string.prayer_isha) to timings.Isha
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
            nextItem = getApplication<Application>().getString(R.string.prayer_fajr) to timings.Fajr
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
