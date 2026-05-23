package com.example.utils

import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.nio.charset.StandardCharsets

object LicenseManager {
    const val FIXED_SECRET_WORD = "ODOO_PHONE_REPAIR_2026"
    const val FIXED_XOR_KEY = "SECRET_XOR_KEY_99"

    /**
     * Get unique device ID on Android (Secure Android ID)
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "REPAIR_FALLBACK_ID_77"
    }

    /**
     * Decrypt and verify the activation key. Returns true if valid.
     */
    fun verifyActivationKey(context: Context, key: String): Boolean {
        return try {
            val deviceId = getDeviceId(context)
            val trimmedKey = key.trim().replace("\n", "").replace("\r", "")
            val decodedBytes = Base64.decode(trimmedKey, Base64.DEFAULT)
            val keyBytes = FIXED_XOR_KEY.toByteArray(StandardCharsets.UTF_8)
            
            val decryptedBytes = ByteArray(decodedBytes.size)
            for (i in decodedBytes.indices) {
                decryptedBytes[i] = (decodedBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            
            val combined = String(decryptedBytes, StandardCharsets.UTF_8)
            val parts = combined.split("|")
            if (parts.size == 3) {
                val decodedDeviceId = parts[0]
                val decodedYear = parts[1]
                val decodedSecret = parts[2]
                
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val licenseYear = decodedYear.toIntOrNull() ?: 0
                
                decodedDeviceId == deviceId && licenseYear >= currentYear && decodedSecret == FIXED_SECRET_WORD
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
