package com.example.utils

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import java.util.concurrent.TimeUnit

object LicenseManager {

    private const val PREFS_FILE_NAME = "sys_runtime_conf"
    // Obfuscated key names for protection against simple decompilers
    private const val KEY_S = "sys_init_s" // status (Boolean: true/false)
    private const val KEY_K = "sys_init_k" // license key string

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    /**
     * Get unique device ID on Android (Secure Android ID)
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "REPAIR_FALLBACK_ID_77"
    }

    /**
     * Initialize secure shared preferences under EncryptedSharedPreferences
     */
    private fun getEncryptedPrefs(context: Context) = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback or handle master key key store corruption gracefully
        context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Local cached check for extremely fast UI renderings
     */
    fun isLocallyVerified(context: Context): Boolean {
        val prefs = getEncryptedPrefs(context)
        return prefs.getBoolean(KEY_S, false)
    }

    fun getSavedLicenseKey(context: Context): String {
        val prefs = getEncryptedPrefs(context)
        return prefs.getString(KEY_K, "") ?: ""
    }

    /**
     * Save the validated state into encrypted storage
     */
    private fun saveSecurityState(context: Context, status: Boolean, key: String) {
        val prefs = getEncryptedPrefs(context)
        prefs.edit()
            .putBoolean(KEY_S, status)
            .putString(KEY_K, key)
            .apply()
    }

    /**
     * Activation logic via Cloudflare workers (POST API)
     * Obfuscated function naming to hide direct licensing references from simple grep/reverse engineering
     */
    suspend fun executeSecuredInitSequence(context: Context, key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val trimmedKey = key.trim()

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val payloadMap = mapOf(
                "deviceId" to deviceId,
                "licenseKey" to trimmedKey
            )
            val jsonAdapter = moshi.adapter(Map::class.java)
            val jsonPayload = jsonAdapter.toJson(payloadMap)

            val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://black-sunset-86d2.bouaffar00.workers.dev/api/activate")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val responseMap = moshi.adapter(Map::class.java).fromJson(bodyString)
                val isSuccess = responseMap?.get("success") == true || responseMap?.get("valid") == true
                if (isSuccess) {
                    saveSecurityState(context, true, trimmedKey)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Periodic background validation/checking (GET API)
     * Obfuscated verify function
     */
    suspend fun runIntegrityCheckRoutine(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId(context)
            val key = getSavedLicenseKey(context)
            if (key.isBlank()) {
                saveSecurityState(context, false, "")
                return@withContext false
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val url = "https://black-sunset-86d2.bouaffar00.workers.dev/api/verify?licenseKey=$key&deviceId=$deviceId"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val responseMap = moshi.adapter(Map::class.java).fromJson(bodyString)
                val isValid = responseMap?.get("valid") == true || responseMap?.get("success") == true
                saveSecurityState(context, isValid, if (isValid) key else "")
                isValid
            } else {
                // If network failure / timeout occurs, we maintain cached offline license for smooth user experience,
                // BUT we enforce true/false based on HTTP response status if the server explicitly replied with an error.
                if (response.code == 400 || response.code == 401 || response.code == 403) {
                    saveSecurityState(context, false, "")
                    false
                } else {
                    isLocallyVerified(context)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Keep the offline cache on direct network exception/timeout to avoid blocking valid users on bad signals.
            isLocallyVerified(context)
        }
    }
}
