package com.asyria.security.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.asyria.security.R
import java.util.concurrent.TimeUnit

class AdhanWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val prayerName = inputData.getString("PRAYER_NAME") ?: "Adhan"
        showNotification(prayerName)
        return Result.success()
    }

    private fun showNotification(prayerName: String) {
        val channelId = "adhan_channel"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Adhan Notifications", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val verses = listOf(
            "\"وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ\" (57:4)",
            "\"أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ\" (13:28)",
            "\"إِنَّ مَعَ الْعُسْرِ يُسْرًا\" (94:6)"
        )
        val selectedVerse = verses.random()

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Using system icon for now
            .setContentTitle("Time for $prayerName")
            .setContentText(selectedVerse)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        fun schedule(context: Context, prayerName: String, delayMillis: Long) {
            val data = Data.Builder()
                .putString("PRAYER_NAME", prayerName)
                .build()

            val request = OneTimeWorkRequestBuilder<AdhanWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "adhan_$prayerName",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
