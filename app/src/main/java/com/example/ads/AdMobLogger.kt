package com.example.ads

import android.util.Log
import com.google.android.gms.ads.LoadAdError

/**
 * AdMobLogger provides structured logging to help debug AdMob configuration,
 * initialization, and ad load status.
 */
object AdMobLogger {
    private const val TAG = "AuraScanAdMob"

    fun logInit(message: String) {
        Log.d(TAG, "Initialization: $message")
    }

    fun logAdLoad(adUnitId: String, adType: String, success: Boolean, error: LoadAdError? = null) {
        if (success) {
            Log.d(TAG, "Load Success - $adType | UnitId: $adUnitId")
        } else {
            // Log code for debugging: 
            // 3: No Fill, 1: Invalid Request, 2: Network Error, 0: Internal Error
            Log.w(TAG, "Load Failed - $adType | UnitId: $adUnitId | Error: ${error?.message} | Code: ${error?.code}")
        }
    }

    fun logConfig(message: String) {
        Log.d(TAG, "Configuration: $message")
    }
}
