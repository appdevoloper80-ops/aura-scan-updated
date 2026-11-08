package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _canRequestAds = MutableStateFlow(consentInformation.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _isPrivacyOptionsRequired = MutableStateFlow(
        consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    )
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    /**
     * Request consent information update and show consent form if required.
     * Startup Order rule:
     * 1. Request consent info with UMP
     * 2. Show consent form if required
     * 3. Only when consentInformation.canRequestAds() is true, initialize ads.
     */
    fun requestConsent(
        activity: Activity,
        onConsentComplete: (canRequestAds: Boolean) -> Unit
    ) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // On consent info update success, load and show form if required
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w("ConsentManager", "Consent form error: ${formError.message}")
                    }

                    val canRequest = consentInformation.canRequestAds()
                    _canRequestAds.value = canRequest
                    _isPrivacyOptionsRequired.value =
                        consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

                    onConsentComplete(canRequest)
                }
            },
            { requestConsentError ->
                Log.w("ConsentManager", "Consent update error: ${requestConsentError.message}")
                val canRequest = consentInformation.canRequestAds()
                _canRequestAds.value = canRequest
                _isPrivacyOptionsRequired.value =
                    consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

                onConsentComplete(canRequest)
            }
        )
    }

    /**
     * Show the UMP privacy options form when requested by the user in Settings.
     */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onDismiss: () -> Unit
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w("ConsentManager", "Privacy options form error: ${formError.message}")
            }
            _canRequestAds.value = consentInformation.canRequestAds()
            _isPrivacyOptionsRequired.value =
                consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            onDismiss()
        }
    }
}
