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
          .client(
              okhttp3.OkHttpClient.Builder()
                  .connectTimeout(20, TimeUnit.SECONDS)
                  .readTimeout(20, TimeUnit.SECONDS)
                  .writeTimeout(20, TimeUnit.SECONDS)
                  .build()
          )
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

    private fun getComprehensiveSupplications(): List<SupplicationEntity> {
        val app = getApplication<Application>()
        return listOf(
            // Morning & Evening
            SupplicationEntity(category = app.getString(R.string.supplication_category_morning_evening), content = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.", translation = "We have reached the evening and at this very time unto God belongs all sovereignty, and all praise is for God...", resonance = "A declaration of God's sovereignty at the end of the day."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_morning_evening), content = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.", translation = "O Allah, by You we enter the morning and by You we enter the evening, by You we live and by You we die, and to You is the resurrection.", resonance = "A testament to reliance on God for the cycles of life."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_morning_evening), content = "سَيِّدُ الاِسْتِغْفَارِ: اللَّهُمَّ أَنْتَ رَبِّي لاَ إِلَهَ إِلاَّ أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ.", translation = "The master of seeking forgiveness: O Allah, You are my Lord, none has the right to be worshipped except You. You created me and I am Your servant...", resonance = "The highest form of seeking forgiveness."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_morning_evening), content = "أَعُوذُ بِكَلِمَاتِ اللهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.", translation = "I seek refuge in the perfect words of Allah from the evil of what He has created.", resonance = "A powerful shield against all forms of harm."),

            // Quranic Prayers
            SupplicationEntity(category = app.getString(R.string.supplication_category_quranic), content = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ.", translation = "Our Lord, give us in this world [that which is] good and in the Hereafter [that which is] good and protect us from the punishment of the Fire.", resonance = "A comprehensive prayer for the best of both worlds."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_quranic), content = "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً.", translation = "Our Lord, let not our hearts deviate after You have guided us and grant us from Yourself mercy.", resonance = "A plea for steadfastness on the right path."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_quranic), content = "رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي.", translation = "My Lord, expand for me my breast [with assurance] and ease for me my task.", resonance = "Prophet Moses' prayer for strength and clarity."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_quranic), content = "رَبِّ زِدْنِي عِلْمًا.", translation = "My Lord, increase me in knowledge.", resonance = "A simple yet profound request for intellectual and spiritual growth."),

            // Comprehensive Supplications (Jawami' al-Du'a)
            SupplicationEntity(category = app.getString(R.string.supplication_category_comprehensive), content = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ.", translation = "O Allah, I ask You for forgiveness and well-being in this world and the Hereafter.", resonance = "Seeking pardon and holistic well-being."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_comprehensive), content = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ.", translation = "O Allah, help me to remember You, to give You thanks, and to worship You in the best of manners.", resonance = "A request for divine assistance in worship."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_comprehensive), content = "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ.", translation = "O Turner of the hearts, make my heart firm upon Your religion.", resonance = "A crucial prayer for stability of faith."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_comprehensive), content = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ زَوَالِ نِعْمَتِكَ، وَتَحَوُّلِ عَافِيَتِكَ، وَفُجَاءَةِ نِقْمَتِكَ، وَجَمِيعِ سَخَطِكَ.", translation = "O Allah, I seek refuge in You from the decline of Your favor, the passing of Your wellness, the suddenness of Your vengeance, and all of Your wrath.", resonance = "A protection against loss and sudden calamity."),
            
            // For Ease & Soul Calming
            SupplicationEntity(category = app.getString(R.string.supplication_category_soul_calming), content = "لا إِلَهَ إِلا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ.", translation = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.", resonance = "Prophet Jonah's prayer from the belly of the whale; for distress."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_soul_calming), content = "اللَّهُمَّ رَحْمَتَكَ أَرْجُو فَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ، وَأَصْلِحْ لِي شَأْنِي كُلَّهُ.", translation = "O Allah, it is Your mercy that I hope for, so do not leave me to myself for the blink of an eye, and rectify all my affairs for me.", resonance = "A plea for divine intervention and support."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_soul_calming), content = "اللَّهُمَّ اكْفِنِي بِحَلَالِكَ عَنْ حَرَامِكَ، وَأَغْنِنِي بِفَضْلِكَ عَمَّنْ سِوَاكَ.", translation = "O Allah, make what is lawful enough for me, as opposed to what is unlawful, and spare me by Your grace from needing anyone other than You.", resonance = "A prayer for halal provision and independence."),

            // For Knowledge & Wisdom
            SupplicationEntity(category = app.getString(R.string.supplication_category_wisdom), content = "اللَّهُمَّ انْفَعْنِي بِمَا عَلَّمْتَنِي، وَعَلِّمْنِي مَا يَنْفَعُنِي، وَزِدْنِي عِلْمًا.", translation = "O Allah, benefit me with what You have taught me, teach me what will benefit me, and increase me in knowledge.", resonance = "A prayer for beneficial knowledge."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_wisdom), content = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا.", translation = "O Allah, I ask You for beneficial knowledge, goodly provision, and acceptable deeds.", resonance = "The three pillars of a successful day."),

            // For Forgiveness
            SupplicationEntity(category = app.getString(R.string.supplication_category_forgiveness), content = "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي.", translation = "O Allah, You are Pardoning and love to pardon, so pardon me.", resonance = "A key prayer, especially in the last ten nights of Ramadan."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_forgiveness), content = "أَسْتَغْفِرُ اللهَ الَّذِي لاَ إِلَهَ إِلاَّ هُوَ الْحَىُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ.", translation = "I seek the forgiveness of Allah, whom there is none worthy of worship except Him, the Living, the Eternal, and I repent unto Him.", resonance = "A powerful formula for seeking forgiveness."),

            // For Family & Parents
            SupplicationEntity(category = app.getString(R.string.supplication_category_family), content = "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِنْ ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ.", translation = "My Lord, make me an establisher of prayer, and [many] from my descendants. Our Lord, and accept my supplication.", resonance = "Prophet Abraham's prayer for his lineage."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_family), content = "رَبِّ أَوْزِعْنِي أَنْ أَشْكُرَ نِعْمَتَكَ الَّتِي أَنْعَمْتَ عَلَيَّ وَعَلَىٰ وَالِدَيَّ.", translation = "My Lord, enable me to be grateful for Your favor which You have bestowed upon me and upon my parents.", resonance = "A Quranic prayer for gratitude."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_family), content = "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا.", translation = "My Lord, have mercy upon them as they brought me up [when I was] small.", resonance = "A prayer for one's parents."),

            // For Protection
            SupplicationEntity(category = app.getString(R.string.supplication_category_protection), content = "بِسْمِ اللهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.", translation = "In the name of Allah with whose name nothing is harmed on earth nor in the heavens and He is The All-Hearing, The All-Knowing.", resonance = "A powerful protection against any harm."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_protection), content = "اللَّهُمَّ احْفَظْنِي مِنْ بَيْنِ يَدَيَّ، وَمِنْ خَلْفِي، وَعَنْ يَمِينِي، وَعَنْ شِمَالِي، وَمِنْ فَوْقِي، وَأَعُوذُ بِعَظَمَتِكَ أَنْ أُغْتَالَ مِنْ تَحْتِي.", translation = "O Allah, protect me from before me, and from behind me, and on my right, and on my left, and from above me...", resonance = "A comprehensive request for protection from all directions."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_protection), content = "اللَّهُمَّ فَاطِرَ السَّمَوَاتِ وَالْأَرْضِ، عَالِمَ الْغَيْبِ وَالشَّهَادَةِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا أَنْتَ.", translation = "O Allah, Creator of the heavens and the earth, Knower of the unseen and the witnessed, Lord and Sovereign of all things...", resonance = "A declaration of faith and seeking refuge."),
            
            // Miscellaneous
            SupplicationEntity(category = app.getString(R.string.supplication_category_misc), content = "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ.", translation = "O Allah, send your prayer upon Muhammad and the family of Muhammad.", resonance = "Sending blessings upon the Prophet (ﷺ)."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_misc), content = "سُبْحَانَ اللهِ وَبِحَمْدِهِ، عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.", translation = "Glory is to Allah and praise is to Him, by the number of His creation, by His pleasure, by the weight of His throne, and by the ink of His words.", resonance = "A comprehensive and weighty form of praise."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_misc), content = "لا حَوْلَ وَلا قُوَّةَ إِلا بِاللهِ.", translation = "There is no might nor power except with Allah.", resonance = "A treasure from the treasures of Paradise."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_misc), content = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مُسْلِمِينَ.", translation = "Praise be to Allah Who has fed us and given us drink, and made us Muslims.", resonance = "Gratitude for the blessings of sustenance and faith."),
            SupplicationEntity(category = app.getString(R.string.supplication_category_misc), content = "سُبْحَانَكَ اللَّهُمَّ وَبِحَمْدِكَ، أَشْهَدُ أَنْ لاَ إِلَهَ إِلاَّ أَنْتَ، أَسْتَغْفِرُكَ وَأَتُوبُ إِلَيْكَ.", translation = "Glory is to You, O Allah, and praise. I bear witness that there is none worthy of worship but You. I seek Your forgiveness and repent to You.", resonance = "Expiation for sins committed in a gathering.")
        )
    }

    private fun loadSupplications() {
        viewModelScope.launch {
            val list = db.supplicationDao().getAllSupplications()
            if (list.isEmpty()) {
                val seed = getComprehensiveSupplications()
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
                    val response = api.getTimings(getApplication<Application>().getString(R.string.damascus), getApplication<Application>().getString(R.string.syria), 4)
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
