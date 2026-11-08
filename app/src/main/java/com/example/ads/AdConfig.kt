package com.example.ads

import com.example.BuildConfig

/**
 * AdConfig stores all AdMob test IDs read directly from BuildConfig fields.
 * Release ad unit IDs can later be swapped into build.gradle.kts without touching code.
 */
object AdConfig {
    val appId: String = BuildConfig.ADMOB_APP_ID
    val bannerAdUnitId: String = BuildConfig.ADMOB_BANNER_ID
    val interstitialAdUnitId: String = BuildConfig.ADMOB_INTERSTITIAL_ID
    val rewardedAdUnitId: String = BuildConfig.ADMOB_REWARDED_ID
    val rewardedInterstitialAdUnitId: String = BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID

    // Configurable list of test device IDs (empty by default)
    var testDeviceIds: List<String> = emptyList()
}
