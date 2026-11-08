package com.example

import android.app.Application
import com.example.ads.AdMobManager

class AuraScanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob SDK and start preloading ads on application startup
        AdMobManager.initialize(this, canRequestAds = true)
    }
}
