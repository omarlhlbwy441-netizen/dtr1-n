package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class RafeeqApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebaseSafely()
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:361359369285:android:rafeeqstore")
                        .setApiKey("AIzaSyDemoKeyForRafeeqStore123456")
                        .setProjectId("rafeeq-store-app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            }
        } catch (e: Exception) {
            Log.e("RafeeqApplication", "Firebase initialization fallback: ${e.message}")
            try {
                if (FirebaseApp.getApps(this).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:361359369285:android:rafeeqstore")
                        .setApiKey("AIzaSyDemoKeyForRafeeqStore123456")
                        .setProjectId("rafeeq-store-app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                }
            } catch (ex: Exception) {
                Log.e("RafeeqApplication", "Critical Firebase init failure: ${ex.message}")
            }
        }
    }
}
