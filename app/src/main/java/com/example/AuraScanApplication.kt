package com.example

import android.app.Application
import android.util.Log
import com.example.ads.AdMobManager

class AuraScanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Safely initialize AdMob SDK on application startup
            AdMobManager.initialize(this, canRequestAds = true)
        } catch (e: Throwable) {
            Log.e("AuraScanApplication", "AdMob initialization warning: ${e.localizedMessage}", e)
        }
    }
}
