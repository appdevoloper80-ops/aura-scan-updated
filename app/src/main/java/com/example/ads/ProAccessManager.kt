package com.example.ads

import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * State-managed manager that tracks rewarded ads watched and handles the 5-minute Pro unlock logic.
 * Source of truth for Pro status, ad progress, and the countdown timer.
 */
class ProAccessManager(
    private val preferencesRepository: UserPreferencesRepository,
    private val scope: CoroutineScope
) {
    companion object {
        const val ADS_THRESHOLD = 5
        const val PRO_DURATION_MINUTES = 5
        const val PRO_DURATION_MILLIS = PRO_DURATION_MINUTES * 60 * 1000L
    }

    // Current ads watched count (0 to ADS_THRESHOLD)
    val adsWatchedCount: StateFlow<Int> = preferencesRepository.rewardedAdsWatchedCount
        .stateIn(scope, SharingStarted.Eagerly, 0)

    // Whether Pro is currently active
    val isProUnlocked: StateFlow<Boolean> = preferencesRepository.isProUnlocked
        .stateIn(scope, SharingStarted.Eagerly, false)

    // Formatted countdown timer string (e.g., "04:59")
    private val _remainingTimeFormatted = MutableStateFlow("")
    val remainingTimeFormatted: StateFlow<String> = _remainingTimeFormatted.asStateFlow()

    private var timerJob: Job? = null
    private var lastKnownExpiry = 0L

    init {
        // Collect expiry updates to manage the countdown timer
        scope.launch {
            preferencesRepository.proExpiryTimestamp.collect { expiry ->
                lastKnownExpiry = expiry
                if (expiry > System.currentTimeMillis()) {
                    startTimerTicker(expiry)
                } else {
                    _remainingTimeFormatted.value = ""
                    timerJob?.cancel()
                }
            }
        }
    }

    /**
     * Called when a rewarded ad is fully watched.
     * Increments count and unlocks Pro if threshold is reached.
     */
    fun onAdWatched(
        onProUnlocked: (String) -> Unit,
        onProgressUpdated: (Int) -> Unit
    ) {
        scope.launch {
            val newCount = preferencesRepository.incrementRewardedAdsCount()
            if (newCount >= ADS_THRESHOLD) {
                preferencesRepository.resetRewardedAdsCount()
                preferencesRepository.grantProAccess(PRO_DURATION_MILLIS)
                onProUnlocked("$PRO_DURATION_MINUTES minutes")
            } else {
                onProgressUpdated(newCount)
            }
        }
    }

    /**
     * Starts a per-second job to update the remainingTimeFormatted flow.
     */
    private fun startTimerTicker(expiry: Long) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val diff = expiry - now
                if (diff > 0) {
                    val min = TimeUnit.MILLISECONDS.toMinutes(diff)
                    val sec = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                    _remainingTimeFormatted.value = String.format("%02d:%02d", min, sec)
                } else {
                    _remainingTimeFormatted.value = ""
                    break
                }
                delay(1000)
            }
        }
    }

    /**
     * Manual check if Pro is ready (e.g., for UI buttons).
     */
    fun isProCurrentlyActive(): Boolean = isProUnlocked.value
}
