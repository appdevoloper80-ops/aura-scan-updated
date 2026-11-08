package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val HAS_CONFIRMED_AGE = booleanPreferencesKey("has_confirmed_age")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val IS_PRO_UNLOCKED = booleanPreferencesKey("is_pro_unlocked")
        val PRO_EXPIRY_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("pro_expiry_timestamp")
        val ACTION_COUNT = androidx.datastore.preferences.core.intPreferencesKey("action_count")
        val REWARDED_ADS_WATCHED_COUNT = androidx.datastore.preferences.core.intPreferencesKey("rewarded_ads_watched_count")
        val APP_THEME_DARK = booleanPreferencesKey("app_theme_dark")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val hasConfirmedAge: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_CONFIRMED_AGE] ?: false
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    val isProUnlocked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val expiry = preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] ?: 0L
        val legacyPro = preferences[PreferencesKeys.IS_PRO_UNLOCKED] ?: false
        legacyPro || (System.currentTimeMillis() < expiry)
    }

    val proExpiryTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] ?: 0L
    }

    val rewardedAdsWatchedCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REWARDED_ADS_WATCHED_COUNT] ?: 0
    }

    val proRemainingMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        val expiry = preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] ?: 0L
        val diff = expiry - System.currentTimeMillis()
        if (diff > 0) ((diff / 60000L) + 1).toInt() else 0
    }

    val actionCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTION_COUNT] ?: 0
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_THEME_DARK] ?: true
    }

    val isHighContrast: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HIGH_CONTRAST] ?: false
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "en"
    }

    suspend fun setAgeConfirmed(confirmed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_CONFIRMED_AGE] = confirmed
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    suspend fun setProUnlocked(unlocked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PRO_UNLOCKED] = unlocked
        }
    }

    suspend fun grantProAccess(durationMillis: Long = 30 * 60 * 1000L) {
        val now = System.currentTimeMillis()
        context.dataStore.edit { preferences ->
            val currentExpiry = preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] ?: 0L
            val baseTime = if (currentExpiry > now) currentExpiry else now
            preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] = baseTime + durationMillis
            preferences[PreferencesKeys.IS_PRO_UNLOCKED] = false // rely on timestamp
        }
    }

    suspend fun incrementActionCount(): Int {
        var newCount = 0
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.ACTION_COUNT] ?: 0
            newCount = current + 1
            preferences[PreferencesKeys.ACTION_COUNT] = newCount
        }
        return newCount
    }

    suspend fun incrementRewardedAdsCount(): Int {
        var newCount = 0
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.REWARDED_ADS_WATCHED_COUNT] ?: 0
            newCount = current + 1
            preferences[PreferencesKeys.REWARDED_ADS_WATCHED_COUNT] = newCount
        }
        return newCount
    }

    suspend fun resetRewardedAdsCount() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REWARDED_ADS_WATCHED_COUNT] = 0
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME_DARK] = isDark
        }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_CONTRAST] = enabled
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language
        }
    }
}
