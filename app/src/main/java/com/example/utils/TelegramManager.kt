package com.example.utils

import android.util.Log
import com.example.database.Ticket
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

object TelegramManager {
    private val client = OkHttpClient()

    fun sendNewTicket(ticket: Ticket, botToken: String, chatId: String) {
        if (botToken.isBlank() || chatId.isBlank()) {
            return
        }

        val caption = """
            📦 <b>تذكرة صيانة جديدة #TK-${1000 + ticket.id}</b>

            👤 <b>العميل:</b> ${ticket.customerName}
            📞 <b>الهاتف:</b> ${ticket.customerPhone}
            📱 <b>الجهاز:</b> ${ticket.deviceModel}
            🛠️ <b>العطل المحدد:</b> ${ticket.faultDescription}
            💵 <b>السعر الإجمالي:</b> ${ticket.totalPrice}
            💰 <b>الدفعة المقدمة:</b> ${ticket.advancePayment}
            💳 <b>المبلغ المتبقي:</b> ${ticket.remainingAmount}
            📝 <b>ملاحظات:</b> ${ticket.notes.ifBlank { "لا توجد" }}
        """.trimIndent()

        // Gather path of images
        val images = mutableListOf<String>()
        ticket.frontImagePath?.let { if (it.isNotBlank()) images.add(it) }
        ticket.backImagePath?.let { if (it.isNotBlank()) images.add(it) }

        if (images.isEmpty()) {
            // Send simple HTML message
            try {
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val jsonPayload = """
                    {
                        "chat_id": "$chatId",
                        "text": ${escapeJsonString(caption)},
                        "parse_mode": "HTML"
                    }
                """.trimIndent()
                val body = jsonPayload.toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/sendMessage")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("TelegramManager", "Failed to send msg: ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("TelegramManager", "Error sending message", e)
            }
        } else {
            // Send first image with caption
            try {
                val firstImgFile = File(images[0])
                if (firstImgFile.exists()) {
                    val fileBody = firstImgFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("chat_id", chatId)
                        .addFormDataPart("photo", firstImgFile.name, fileBody)
                        .addFormDataPart("caption", caption)
                        .addFormDataPart("parse_mode", "HTML")
                        .build()

                    val request = Request.Builder()
                        .url("https://api.telegram.org/bot$botToken/sendPhoto")
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.e("TelegramManager", "Failed to send photo: ${response.body?.string()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TelegramManager", "Error sending first photo", e)
            }

            // Send remaining images if any
            for (i in 1 until images.size) {
                try {
                    val imgFile = File(images[i])
                    if (imgFile.exists()) {
                        val fileBody = imgFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        val requestBody = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("chat_id", chatId)
                            .addFormDataPart("photo", imgFile.name, fileBody)
                            .addFormDataPart("caption", "صورة ملحقة للجهاز #${1000 + ticket.id}")
                            .build()

                        val request = Request.Builder()
                            .url("https://api.telegram.org/bot$botToken/sendPhoto")
                            .post(requestBody)
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                Log.e("TelegramManager", "Failed to send back page photo: ${response.body?.string()}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TelegramManager", "Error sending other photos", e)
                }
            }
        }
    }

    private fun escapeJsonString(input: String): String {
        val builder = StringBuilder()
        builder.append("\"")
        for (c in input) {
            when (c) {
                '\"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\t' -> builder.append("\\t")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                else -> {
                    if (c.code < 32) {
                        builder.append(String.format("\\u%04x", c.code))
                    } else {
                        builder.append(c)
                    }
                }
            }
        }
        builder.append("\"")
        return builder.toString()
    }
}
