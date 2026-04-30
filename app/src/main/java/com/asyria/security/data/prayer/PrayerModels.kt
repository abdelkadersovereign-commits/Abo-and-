package com.asyria.security.data.prayer

import androidx.room.*

@Entity(tableName = "prayer_times")
data class PrayerTimesEntity(
    @PrimaryKey val date: String, // format yyyy-MM-dd
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

@Entity(tableName = "supplications")
data class SupplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val content: String,
    val translation: String,
    val resonance: String // High-tech artistic label for the effect
)

data class PrayerApiResponse(
    val code: Int,
    val status: String,
    val data: PrayerData
)

data class PrayerData(
    val timings: Timings,
    val date: DateInfo,
    val meta: MetaInfo
)

data class Timings(
    val Fajr: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String
)

data class DateInfo(
    val readable: String,
    val timestamp: String
)

data class MetaInfo(
    val timezone: String
)
