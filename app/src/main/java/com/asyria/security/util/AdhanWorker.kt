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
import java.util.concurrent.TimeUnit

class AdhanWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // جلب اسم الصلاة من البيانات المدخلة
        val prayerName = inputData.getString("PRAYER_NAME") ?: "Adhan"
        
        return try {
            showNotification(prayerName)
            Result.success()
        } catch (e: Exception) {
            // إعادة المحاولة في حال حدوث خطأ مفاجئ لضمان دقة مواعيد الصلاة
            Result.retry()
        }
    }

    private fun showNotification(prayerName: String) {
        val channelId = "adhan_channel"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // إنشاء قناة الإشعارات لأنظمة أندرويد الحديثة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Adhan Notifications", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for prayer times and daily verses"
                enableLights(true)
                // تعيين نغمة هادئة تماشياً مع مواصفات التطبيق
                setSound(null, null) 
            }
            manager.createNotificationChannel(channel)
        }

        // الآيات القرآنية الترحيبية كما طلبت في مواصفات A.SYRIA
        val verses = listOf(
            "\"وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ\" (57:4)",
            "\"أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ\" (13:28)",
            "\"إِنَّ مَعَ الْعُسْرِ يُسْرًا\" (94:6)",
            "\"وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ\" (2:45)"
        )
        val selectedVerse = verses.random()

        // بناء الإشعار بتصميم متطور
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان الآن موعد صلاة $prayerName")
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
                .addTag("adhan_tag") // وسم لتسهيل إدارة المهام
                .build()

            // استخدام REPLACE لضمان عدم تكرار الإشعارات لنفس الصلاة
            WorkManager.getInstance(context).enqueueUniqueWork(
                "adhan_$prayerName",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
