package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val ticketId = inputData.getLong("ticket_id", -1L)
            val deviceModel = inputData.getString("device_model") ?: "جهاز صيانة"
            val ringtoneUriStr = inputData.getString("ringtone_uri")

            if (ticketId == -1L) return@withContext Result.failure()

            val database = AppDatabase.getDatabase(applicationContext)
            val ticket = database.ticketDao().getTicketById(ticketId)

            // Verify if the slot/device is still actively under maintenance (IN_PROGRESS)
            if (ticketId == 9999L || (ticket != null && ticket.status == "IN_PROGRESS")) {
                showNotification(ticketId, deviceModel)
                playRingtone(ringtoneUriStr)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showNotification(ticketId: Long, deviceModel: String) {
        val channelId = "device_maintenance_timer"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تذكير مؤقت صيانة الأجهزة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قنوات تنبيهات تفيد بانتهاء الوقت المبرمج لإصلاح جهاز معین"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("انقضى وقت صيانة الجهاز!")
            .setContentText("انتهى الموعد المحدد لصيانة جهازك: $deviceModel (#TK-${1000 + ticketId})")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(ticketId.toInt() + 10000, builder.build())
    }

    private fun playRingtone(uriStr: String?) {
        try {
            val ringtoneUri = if (!uriStr.isNullOrEmpty()) {
                Uri.parse(uriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            
            val ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
