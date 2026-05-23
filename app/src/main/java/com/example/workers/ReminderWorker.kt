package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val tickets = database.ticketDao().getAllTicketsSync()
            
            // Check for tickets in a pending/repairing state built more than 24h ago
            val currentTime = System.currentTimeMillis()
            val forgottenCount = tickets.count { ticket ->
                (ticket.status == "PENDING" || ticket.status == "IN_PROGRESS") && 
                (currentTime - ticket.createdAt > 24 * 60 * 60 * 1000)
            }
            
            if (forgottenCount > 0) {
                showNotification(forgottenCount)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showNotification(count: Int) {
        val channelId = "phone_repair_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تذكير تذاكر الصيانة المنسية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات للتذاكر التي تجاوزت ٢٤ ساعة دون تسليم"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("تنبيه تذاكر الصيانة!")
            .setContentText("لديك $count تذاكر معلقة أو منسية لم تسلم بعد. يرجى مراجعتها.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(991, builder.build())
    }
}
