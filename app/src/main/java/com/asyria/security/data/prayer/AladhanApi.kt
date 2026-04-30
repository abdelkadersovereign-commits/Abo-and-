package com.asyria.security.data.prayer

import retrofit2.http.GET
import retrofit2.http.Query

interface AladhanApi {
    @GET("timingsByCity")
    suspend fun getTimings(
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int = 2
    ): PrayerApiResponse

    @GET("timings")
    suspend fun getTimingsByLocation(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 2
    ): PrayerApiResponse

    companion object {
        const val BASE_URL = "https://api.aladhan.com/v1/"
    }
}
