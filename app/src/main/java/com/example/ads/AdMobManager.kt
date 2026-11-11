package com.example.ads

import android.app.Activity
import android.content.Context
import android.widget.Toast
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.AdapterStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

sealed class AdStatus {
    object Idle : AdStatus() { override fun toString() = "Idle" }
    object Loading : AdStatus() { override fun toString() = "Loading..." }
    object Ready : AdStatus() { override fun toString() = "Ready ✓" }
    data class Failed(val message: String, val code: Int) : AdStatus() {
        override fun toString() = "Failed (Code $code: $message)"
    }
}

object AdMobManager {

    private val isInitializing = AtomicBoolean(false)
    private val _initStatus = MutableStateFlow<AdStatus>(AdStatus.Idle)
    val initStatus: StateFlow<AdStatus> = _initStatus.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _bannerStatus = MutableStateFlow<AdStatus>(AdStatus.Idle)
    val bannerStatus: StateFlow<AdStatus> = _bannerStatus.asStateFlow()

    private val _interstitialStatus = MutableStateFlow<AdStatus>(AdStatus.Idle)
    val interstitialStatus: StateFlow<AdStatus> = _interstitialStatus.asStateFlow()

    private val _rewardedStatus = MutableStateFlow<AdStatus>(AdStatus.Idle)
    val rewardedStatus: StateFlow<AdStatus> = _rewardedStatus.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null

    private val _isRewardedLoading = MutableStateFlow(false)
    val isRewardedLoading: StateFlow<Boolean> = _isRewardedLoading.asStateFlow()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("AdMobManager", "AdMob coroutine caught uncaught exception: ${throwable.localizedMessage}", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + coroutineExceptionHandler)

    fun updateBannerStatus(status: AdStatus) {
        _bannerStatus.value = status
    }

    fun initialize(context: Context, canRequestAds: Boolean = true) {
        if (!canRequestAds) {
            _initStatus.value = AdStatus.Failed("Consent not granted", -1)
            return
        }
        if (isInitializing.getAndSet(true)) return

        _initStatus.value = AdStatus.Loading
        scope.launch {
            try {
                AdMobLogger.logConfig("Initializing MobileAds SDK with RequestConfiguration")
                val requestConfig = RequestConfiguration.Builder()
                    .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                    .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                    .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                    .build()
                MobileAds.setRequestConfiguration(requestConfig)
                MobileAds.initialize(context) { statusMap ->
                    try {
                        val adapterMap = statusMap.adapterStatusMap
                        val isReady = adapterMap.values.any { it.initializationState == AdapterStatus.State.READY } || adapterMap.isNotEmpty()
                        _isInitialized.value = true
                        _initStatus.value = AdStatus.Ready
                        isInitializing.set(false)
                        AdMobLogger.logInit("SDK Initialization Complete (Adapters Ready: $isReady)")
                        // Preload ad units only after SDK initialization reports complete
                        preloadAds(context)
                    } catch (e: Throwable) {
                        Log.e("AdMobManager", "Error in MobileAds initialization callback: ${e.localizedMessage}", e)
                        _initStatus.value = AdStatus.Failed(e.message ?: "Initialization error", -1)
                        isInitializing.set(false)
                    }
                }
            } catch (e: Throwable) {
                Log.e("AdMobManager", "Failed to start MobileAds initialization: ${e.localizedMessage}", e)
                _initStatus.value = AdStatus.Failed(e.message ?: "Startup error", -1)
                isInitializing.set(false)
            }
        }
    }

    fun preloadAds(context: Context) {
        if (!_isInitialized.value) {
            AdMobLogger.logConfig("Diagnostic check: MobileAds SDK initialization incomplete. Initializing before loading ad units...")
            initialize(context)
            return
        }
        preloadInterstitial(context)
        preloadRewarded(context)
    }

    fun preloadInterstitial(context: Context) {
        try {
            if (!_isInitialized.value) {
                AdMobLogger.logConfig("Diagnostic check: Deferred Interstitial load - MobileAds SDK initialization incomplete.")
                initialize(context)
                return
            }
            _interstitialStatus.value = AdStatus.Loading
            val request = AdRequest.Builder().build()
            InterstitialAd.load(context, AdConfig.interstitialAdUnitId, request, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _interstitialStatus.value = AdStatus.Ready
                    AdMobLogger.logAdLoad(AdConfig.interstitialAdUnitId, "Interstitial", true)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _interstitialStatus.value = AdStatus.Failed(error.message, error.code)
                    AdMobLogger.logAdLoad(AdConfig.interstitialAdUnitId, "Interstitial", false, error)
                    if (error.code == AdRequest.ERROR_CODE_NO_FILL) {
                        showNoFillToast(context, "Interstitial")
                    }
                }
            })
        } catch (e: Throwable) {
            Log.e("AdMobManager", "Failed to load interstitial ad: ${e.localizedMessage}", e)
            _interstitialStatus.value = AdStatus.Failed(e.message ?: "Load error", -1)
        }
    }

    fun preloadRewarded(context: Context) {
        try {
            if (!_isInitialized.value) {
                AdMobLogger.logConfig("Diagnostic check: Deferred Rewarded load - MobileAds SDK initialization incomplete.")
                initialize(context)
                return
            }
            _isRewardedLoading.value = true
            _rewardedStatus.value = AdStatus.Loading
            val request = AdRequest.Builder().build()
            AdMobLogger.logConfig("Loading RewardedAd unit ID: ${AdConfig.rewardedAdUnitId}")
            
            RewardedAd.load(context, AdConfig.rewardedAdUnitId, request, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedInterstitialAd = null
                    _isRewardedLoading.value = false
                    _rewardedStatus.value = AdStatus.Ready
                    AdMobLogger.logAdLoad(AdConfig.rewardedAdUnitId, "Rewarded", true)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    AdMobLogger.logAdLoad(AdConfig.rewardedAdUnitId, "Rewarded", false, error)
                    
                    try {
                        // Attempt Rewarded Interstitial as fallback if primary rewarded ad returns No Fill
                        AdMobLogger.logConfig("Attempting RewardedInterstitialAd fallback unit ID: ${AdConfig.rewardedInterstitialAdUnitId}")
                        RewardedInterstitialAd.load(context, AdConfig.rewardedInterstitialAdUnitId, request, object : RewardedInterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                                rewardedInterstitialAd = ad
                                _isRewardedLoading.value = false
                                _rewardedStatus.value = AdStatus.Ready
                                AdMobLogger.logAdLoad(AdConfig.rewardedInterstitialAdUnitId, "RewardedInterstitial (Fallback)", true)
                            }
                            override fun onAdFailedToLoad(fallbackError: LoadAdError) {
                                rewardedInterstitialAd = null
                                _isRewardedLoading.value = false
                                _rewardedStatus.value = AdStatus.Failed(fallbackError.message, fallbackError.code)
                                AdMobLogger.logAdLoad(AdConfig.rewardedInterstitialAdUnitId, "RewardedInterstitial (Fallback)", false, fallbackError)
                                if (fallbackError.code == AdRequest.ERROR_CODE_NO_FILL || error.code == AdRequest.ERROR_CODE_NO_FILL) {
                                    showNoFillToast(context, "Rewarded Video")
                                }
                            }
                        })
                    } catch (fallbackEx: Throwable) {
                        Log.e("AdMobManager", "Failed to load fallback rewarded interstitial: ${fallbackEx.localizedMessage}", fallbackEx)
                        _isRewardedLoading.value = false
                        _rewardedStatus.value = AdStatus.Failed(fallbackEx.message ?: "Fallback error", -1)
                    }
                }
            })
        } catch (e: Throwable) {
            Log.e("AdMobManager", "Failed to load rewarded ad: ${e.localizedMessage}", e)
            _isRewardedLoading.value = false
            _rewardedStatus.value = AdStatus.Failed(e.message ?: "Load error", -1)
        }
    }

    fun showInterstitial(activity: Activity, onComplete: () -> Unit) {
        try {
            val ad = interstitialAd
            if (ad != null) {
                interstitialAd = null
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        _interstitialStatus.value = AdStatus.Idle
                        preloadInterstitial(activity)
                        onComplete()
                    }
                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        _interstitialStatus.value = AdStatus.Failed(error.message, error.code)
                        preloadInterstitial(activity)
                        onComplete()
                    }
                }
                ad.show(activity)
            } else {
                onComplete()
            }
        } catch (e: Throwable) {
            Log.e("AdMobManager", "Error showing interstitial: ${e.localizedMessage}", e)
            onComplete()
        }
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onAdUnavailable: (String) -> Unit) {
        try {
            val standardAd = rewardedAd
            val fallbackAd = rewardedInterstitialAd

            if (standardAd != null) {
                rewardedAd = null
                _rewardedStatus.value = AdStatus.Idle
                standardAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        preloadRewarded(activity)
                    }
                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        _rewardedStatus.value = AdStatus.Failed(error.message, error.code)
                        preloadRewarded(activity)
                        onAdUnavailable("Video ad failed: ${error.message}")
                    }
                }
                standardAd.show(activity) { reward ->
                    onRewardEarned()
                }
            } else if (fallbackAd != null) {
                rewardedInterstitialAd = null
                _rewardedStatus.value = AdStatus.Idle
                fallbackAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        preloadRewarded(activity)
                    }
                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        _rewardedStatus.value = AdStatus.Failed(error.message, error.code)
                        preloadRewarded(activity)
                        onAdUnavailable("Video ad failed: ${error.message}")
                    }
                }
                fallbackAd.show(activity) { reward ->
                    onRewardEarned()
                }
            } else {
                preloadRewarded(activity)
                onAdUnavailable("Loading video ad... Please tap again in a moment!")
            }
        } catch (e: Throwable) {
            Log.e("AdMobManager", "Error showing rewarded ad: ${e.localizedMessage}", e)
            onAdUnavailable("Unable to display ad: ${e.localizedMessage}")
        }
    }

    fun openAdInspector(context: Context, onDismissed: (String?) -> Unit) {
        try {
            MobileAds.openAdInspector(context) { error ->
                onDismissed(error?.message)
            }
        } catch (e: Throwable) {
            Log.e("AdMobManager", "Error opening ad inspector: ${e.localizedMessage}", e)
            onDismissed(e.localizedMessage)
        }
    }

    private fun showNoFillToast(context: Context, adFormat: String) {
        scope.launch {
            try {
                Toast.makeText(context, "$adFormat Ad: No ad fill available (Code 3). Please try again shortly.", Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                Log.w("AdMobManager", "Toast display failed: ${e.localizedMessage}")
            }
        }
    }
}
