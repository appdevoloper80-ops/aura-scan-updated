package com.example.ads

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdaptiveBanner(
    canRequestAds: Boolean,
    modifier: Modifier = Modifier
) {
    // If ads are not allowed, collapse immediately
    if (!canRequestAds) return

    val context = LocalContext.current
    var isAdLoaded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admob_adaptive_banner_container"),
        contentAlignment = Alignment.Center
    ) {
        val currentWidthDp = if (maxWidth.value > 0) maxWidth.value.toInt() else 320

        // Create the AdView only once per width change
        val adView = remember(currentWidthDp) {
            AdView(context).apply {
                adUnitId = AdConfig.bannerAdUnitId
                val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, currentWidthDp)
                setAdSize(adSize)
                adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        isAdLoaded = true
                        AdMobManager.updateBannerStatus(AdStatus.Ready)
                        Log.d("AdaptiveBanner", "Ad loaded successfully for width $currentWidthDp")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isAdLoaded = false
                        AdMobManager.updateBannerStatus(AdStatus.Failed(error.message, error.code))
                        Log.w("AdaptiveBanner", "Ad failed to load: ${error.message} (code: ${error.code})")
                    }
                }
            }
        }

        // Trigger loadAd only when canRequestAds is true and only once
        DisposableEffect(adView, canRequestAds) {
            if (canRequestAds) {
                AdMobManager.updateBannerStatus(AdStatus.Loading)
                Log.d("AdaptiveBanner", "Loading ad for width $currentWidthDp")
                adView.loadAd(AdRequest.Builder().build())
            }
            onDispose {
                adView.destroy()
            }
        }

        // Only show the Box (and its height) if the ad is successfully loaded
        if (isAdLoaded) {
            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 4.dp)
            )
        }
    }
}
