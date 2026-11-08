package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

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

    private val safeDataStore: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val hasConfirmedAge: Flow<Boolean> = safeDataStore.map { preferences ->
        preferences[PreferencesKeys.HAS_CONFIRMED_AGE] ?: false
    }

    val hasCompletedOnboarding: Flow<Boolean> = safeDataStore.map { preferences ->
        preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    val isProUnlocked: Flow<Boolean> = safeDataStore.map { preferences ->
        val expiry = preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] ?: 0L
        val legacyPro = preferences[PreferencesKeys.IS_PRO_UNLOCKED] ?: false
        legacyPro || (System.currentTimeMillis() < expiry)
    }

    val proExpiryTimestamp: Flow<Long> = safeDataStore.map { preferences ->
        preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] ?: 0L
    }

    val rewardedAdsWatchedCount: Flow<Int> = safeDataStore.map { preferences ->
        preferences[PreferencesKeys.REWARDED_ADS_WATCHED_COUNT] ?: 0
    }

    val proRemainingMinutes: Flow<Int> = safeDataStore.map { preferences ->
        val expiry = preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] ?: 0L
        val diff = expiry - System.currentTimeMillis()
        if (diff > 0) ((diff / 60000L) + 1).toInt() else 0
    }

    val actionCount: Flow<Int> = safeDataStore.map { preferences ->
        preferences[PreferencesKeys.ACTION_COUNT] ?: 0
    }

    val isDarkTheme: Flow<Boolean> = safeDataStore.map { preferences ->
        preferences[PreferencesKeys.APP_THEME_DARK] ?: true
    }

    val isHighContrast: Flow<Boolean> = safeDataStore.map { preferences ->
        preferences[PreferencesKeys.HIGH_CONTRAST] ?: false
    }

    val appLanguage: Flow<String> = safeDataStore.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "en"
    }

    suspend fun setAgeConfirmed(confirmed: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.HAS_CONFIRMED_AGE] = confirmed
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun setProUnlocked(unlocked: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_PRO_UNLOCKED] = unlocked
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun grantProAccess(durationMillis: Long = 30 * 60 * 1000L) {
        try {
            val now = System.currentTimeMillis()
            context.dataStore.edit { preferences ->
                val currentExpiry = preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] ?: 0L
                val baseTime = if (currentExpiry > now) currentExpiry else now
                preferences[PreferencesKeys.PRO_EXPIRY_TIMESTAMP] = baseTime + durationMillis
                preferences[PreferencesKeys.IS_PRO_UNLOCKED] = false // rely on timestamp
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun incrementActionCount(): Int {
        var newCount = 0
        try {
            context.dataStore.edit { preferences ->
                val current = preferences[PreferencesKeys.ACTION_COUNT] ?: 0
                newCount = current + 1
                preferences[PreferencesKeys.ACTION_COUNT] = newCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newCount
    }

    suspend fun incrementRewardedAdsCount(): Int {
        var newCount = 0
        try {
            context.dataStore.edit { preferences ->
                val current = preferences[PreferencesKeys.REWARDED_ADS_WATCHED_COUNT] ?: 0
                newCount = current + 1
                preferences[PreferencesKeys.REWARDED_ADS_WATCHED_COUNT] = newCount
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newCount
    }

    suspend fun resetRewardedAdsCount() {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.REWARDED_ADS_WATCHED_COUNT] = 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.APP_THEME_DARK] = isDark
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.HIGH_CONTRAST] = enabled
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun setLanguage(language: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.APP_LANGUAGE] = language
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
