package com.foxxy.play.notification

import android.content.Context
import android.util.Log
import com.foxxy.play.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object FoxxyFcmRegistration {

    private const val TAG = "FoxxyFCM"
    private const val API_URL =
        "https://foxxyplay-api.vercel.app/api/app/devices/register"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun register(context: Context, token: String) {
        if (token.isBlank()) return

        Thread {
            try {
                val body = JSONObject().apply {
                    put("token", token)
                    put("appVersion", BuildConfig.VERSION_NAME)
                }

                val request = Request.Builder()
                    .url(API_URL)
                    .post(
                        body.toString().toRequestBody(
                            "application/json".toMediaType()
                        )
                    )
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i(TAG, "Token perangkat berhasil didaftarkan.")
                    } else {
                        Log.e(TAG, "Registrasi token gagal: HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tidak dapat mendaftarkan token FCM.", e)
            }
        }.start()
    }
}
