package com.asyria.security.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import com.asyria.security.R
import java.util.concurrent.TimeUnit

class AdhanWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prayerName = inputData.getString("PRAYER_NAME") ?: applicationContext.getString(R.string.adhan_default_prayer_name)
        
        return try {
            showNotification(prayerName)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(prayerName: String) {
        val channelId = "adhan_channel"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                applicationContext.getString(R.string.adhan_notification_channel_name), 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = applicationContext.getString(R.string.adhan_notification_channel_description)
                enableLights(true)
                setSound(null, null) 
            }
            manager.createNotificationChannel(channel)
        }

        val verses = listOf(
            applicationContext.getString(R.string.quran_verse_1),
            applicationContext.getString(R.string.quran_verse_2),
            applicationContext.getString(R.string.quran_verse_3),
            applicationContext.getString(R.string.quran_verse_4)
        )
        val selectedVerse = verses.random()

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(applicationContext.getString(R.string.prayer_time_notification_title, prayerName))
            .setContentText(selectedVerse)
            .setStyle(NotificationCompat.BigTextStyle().bigText(selectedVerse))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        manager.notify(prayerName.hashCode(), notification)
    }

    companion object {
        fun schedule(context: Context, prayerName: String, delayMillis: Long) {
            val data = Data.Builder()
                .putString("PRAYER_NAME", prayerName)
                .build()

            val request = OneTimeWorkRequestBuilder<AdhanWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("adhan_tag")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "adhan_$prayerName",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
