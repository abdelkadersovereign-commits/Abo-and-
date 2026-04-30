package com.asyria.security.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.Data
import com.asyria.security.R

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // جلب اسم الصلاة أو التنبيه من البيانات المرسلة
        val prayerName = inputData.getString("prayer_name") ?: "Prayer"
        
        return try {
            showNotification(prayerName)
            Result.success()
        } catch (e: Exception) {
            // في حال حدوث خطأ تقني، يطلب النظام إعادة المحاولة لاحقاً لضمان وصول التنبيه
            Result.retry()
        }
    }

    private fun showNotification(prayerName: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prayer_notifications"

        // إعداد قناة الإشعارات المتطورة لنظام أندرويد 8 فما فوق
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "A.SYRIA Spiritual Reminders", 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Advanced prayer time notifications"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // بناء الإشعار بلمسة عصرية تتناسب مع واجهة التطبيق التفاعلية
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("A.SYRIA | مزامنة روحية")
            .setContentText("حان الآن وقت صلاة $prayerName")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // سيتم استبداله تلقائياً بأيقونة التطبيق عند البناء النهائي
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setAutoCancel(true)
            .build()

        // استخدام Hash Code لاسم الصلاة يمنع تداخل الإشعارات ويجعلها منظمة
        notificationManager.notify(prayerName.hashCode(), notification)
    }
}
