package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.ConsentManager
import com.example.ads.AdMobManager
import com.example.data.DocumentRepository
import com.example.data.UserPreferencesRepository
import com.example.data.db.AppDatabase
import com.example.data.model.DocumentEntity
import com.example.engine.PdfCreationConfig
import com.example.engine.PdfEngine
import com.example.util.FileStorageManager
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class NavigateToDocument(val documentId: String) : UiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val fileStorageManager = FileStorageManager(application)
    val documentRepository = DocumentRepository(database.documentDao(), fileStorageManager)
    val preferencesRepository = UserPreferencesRepository(application)

    // Ad Managers
    val consentManager = ConsentManager(application)
    val adMobManager = AdMobManager
    val proAccessManager = com.example.ads.ProAccessManager(preferencesRepository, viewModelScope)

    val canRequestAds: StateFlow<Boolean> = consentManager.canRequestAds
    val isPrivacyOptionsRequired: StateFlow<Boolean> = consentManager.isPrivacyOptionsRequired
    val isRewardedLoading = adMobManager.isRewardedLoading

    val rewardedAdsWatchedCount: StateFlow<Int> = proAccessManager.adsWatchedCount
    val proRemainingTime: StateFlow<String> = proAccessManager.remainingTimeFormatted

    val actionCount: StateFlow<Int> = preferencesRepository.actionCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    val documents: StateFlow<List<DocumentEntity>> = documentRepository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hasConfirmedAge: StateFlow<Boolean> = preferencesRepository.hasConfirmedAge
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true // Avoid flicker, dialog checks actual flow
        )

    val hasCompletedOnboarding: StateFlow<Boolean> = preferencesRepository.hasCompletedOnboarding
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val isPro: StateFlow<Boolean> = proAccessManager.isProUnlocked

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        // Initialization/Preloading is triggered by checkAndRequestConsent
    }

    /**
     * Startup Order:
     * 1. After age check passes, request consent info with UMP.
     * 2. Only when canRequestAds() is true, initialize MobileAds and preload ads.
     */
    fun checkAndRequestConsent(activity: Activity) {
        consentManager.requestConsent(activity) { canRequest ->
            if (canRequest) {
                AdMobManager.initialize(getApplication(), true)
                AdMobManager.preloadAds(getApplication())
            }
        }
    }

    fun isProUnlocked(): Boolean {
        return proAccessManager.isProCurrentlyActive()
    }

    fun grantProAccess(durationMillis: Long, label: String) {
        viewModelScope.launch {
            preferencesRepository.grantProAccess(durationMillis)
            _events.emit(UiEvent.ShowSnackbar("AuraScan Pro unlocked for $label!"))
        }
    }

    fun unlockPro() {
        grantProAccess(30 * 60 * 1000L, "30 minutes")
    }

    fun showRewardedAdForPro(activity: Activity, onAdUnavailable: (String) -> Unit) {
        adMobManager.showRewardedAd(
            activity = activity,
            onRewardEarned = {
                proAccessManager.onAdWatched(
                    onProUnlocked = { label ->
                        viewModelScope.launch {
                            _events.emit(UiEvent.ShowSnackbar("AuraScan Pro unlocked for $label!"))
                        }
                    },
                    onProgressUpdated = { count ->
                        viewModelScope.launch {
                            _events.emit(UiEvent.ShowSnackbar("Ad watched! ($count/5 to unlock Pro)"))
                        }
                    }
                )
            },
            onAdUnavailable = onAdUnavailable
        )
    }

    fun triggerInterstitialAfterPdfAction(activity: Activity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val totalActions = preferencesRepository.incrementActionCount()
            // Using AdMobManager to show interstitial
            adMobManager.showInterstitial(activity, onComplete)
        }
    }

    fun confirmAge(isAdult: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAgeConfirmed(isAdult)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun handleScanSuccess(result: GmsDocumentScanningResult) {
        viewModelScope.launch {
            try {
                val docId = UUID.randomUUID().toString()
                val docDir = fileStorageManager.getDocumentDirectory(docId)

                // Copy PDF if available, or generate from pages
                val targetPdf = File(docDir, "scan.pdf")
                val pdfUri = result.pdf?.uri
                if (pdfUri != null) {
                    fileStorageManager.copyUriToFile(pdfUri, targetPdf)
                }

                val pagePaths = mutableListOf<String>()
                val pages = result.pages ?: emptyList()
                pages.forEachIndexed { index, page ->
                    val pageFile = File(docDir, "page_${index + 1}.jpg")
                    fileStorageManager.copyUriToFile(page.imageUri, pageFile)
                    pagePaths.add(pageFile.absolutePath)
                }

                // If PDF was not generated by scanner, build it using PdfEngine
                if (!targetPdf.exists() && pagePaths.isNotEmpty()) {
                    PdfEngine.createPdfFromImages(
                        context = getApplication(),
                        imageUrisOrPaths = pagePaths,
                        outputPdfFile = targetPdf,
                        config = PdfCreationConfig()
                    )
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val docName = "Scan ${dateFormat.format(Date())}"
                val entity = DocumentEntity(
                    id = docId,
                    name = docName,
                    createdAt = System.currentTimeMillis(),
                    pageCount = if (pagePaths.isNotEmpty()) pagePaths.size else 1,
                    pdfPath = targetPdf.absolutePath,
                    pagePathsJson = pagePaths.joinToString("\n")
                )

                documentRepository.insertDocument(entity)
                _events.emit(UiEvent.NavigateToDocument(docId))
            } catch (e: Exception) {
                e.printStackTrace()
                _events.emit(UiEvent.ShowSnackbar("Failed to process scan: ${e.localizedMessage}"))
            }
        }
    }

    fun renameDocument(id: String, newName: String) {
        viewModelScope.launch {
            documentRepository.renameDocument(id, newName)
            _events.emit(UiEvent.ShowSnackbar("Document renamed"))
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            documentRepository.deleteDocument(id)
            _events.emit(UiEvent.ShowSnackbar("Document deleted"))
        }
    }

    fun createDocumentFromImages(
        imageUris: List<String>,
        config: PdfCreationConfig,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val docId = UUID.randomUUID().toString()
                val docDir = fileStorageManager.getDocumentDirectory(docId)
                val targetPdf = File(docDir, "document.pdf")

                val pagePaths = mutableListOf<String>()
                imageUris.forEachIndexed { index, uriStr ->
                    val pageFile = File(docDir, "page_${index + 1}.jpg")
                    fileStorageManager.copyUriToFile(Uri.parse(uriStr), pageFile)
                    pagePaths.add(pageFile.absolutePath)
                }

                val success = PdfEngine.createPdfFromImages(
                    context = getApplication(),
                    imageUrisOrPaths = pagePaths,
                    outputPdfFile = targetPdf,
                    config = config
                )

                if (success) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val docName = "PDF ${dateFormat.format(Date())}"
                    val entity = DocumentEntity(
                        id = docId,
                        name = docName,
                        createdAt = System.currentTimeMillis(),
                        pageCount = pagePaths.size,
                        pdfPath = targetPdf.absolutePath,
                        pagePathsJson = pagePaths.joinToString("\n")
                    )
                    documentRepository.insertDocument(entity)
                    _events.emit(UiEvent.ShowSnackbar("PDF created successfully"))
                    onSuccess(docId)
                } else {
                    _events.emit(UiEvent.ShowSnackbar("Failed to create PDF"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _events.emit(UiEvent.ShowSnackbar("Error creating PDF: ${e.localizedMessage}"))
            }
        }
    }
}
