package com.foxxy.play

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.foxxy.play.notification.FoxxyFcmRegistration

class PublicFoxxyPlayApp : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FoxxyFcmRegistration.register(this, token)
            }
            .addOnFailureListener { error ->
                Log.e("FoxxyFCM", "Gagal mengambil token FCM.", error)
            }
    }
}
