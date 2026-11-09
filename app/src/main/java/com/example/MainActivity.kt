package com.example

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.UiEvent
import com.example.ui.components.AgeCheckDialog
import com.example.ui.components.ProUnlockDialog
import com.example.ui.screens.AiSummaryScreen
import com.example.ui.screens.DocumentScreen
import com.example.ui.screens.ExportDocumentScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImagesToPdfScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OcrScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PhotoResizerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.TranslatorScreen
import com.example.ui.theme.AuraScanTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AuraScanTheme(darkTheme = true) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val documents by viewModel.documents.collectAsState()
                val hasConfirmedAge by viewModel.hasConfirmedAge.collectAsState()
                val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
                val isProUnlocked by viewModel.isPro.collectAsState()

                // Ad states
                val canRequestAds by viewModel.canRequestAds.collectAsState()
                val isPrivacyOptionsRequired by viewModel.isPrivacyOptionsRequired.collectAsState()
                val isRewardedLoading by viewModel.isRewardedLoading.collectAsState()
                val rewardedAdsWatchedCount by viewModel.rewardedAdsWatchedCount.collectAsState()
                val proRemainingTime by viewModel.proRemainingTime.collectAsState()

                var showProDialog by rememberSaveable { mutableStateOf(false) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Request Consent and Init Ads after Age Check passes
                LaunchedEffect(hasConfirmedAge) {
                    if (hasConfirmedAge) {
                        viewModel.checkAndRequestConsent(this@MainActivity)
                    }
                }

                // ML Kit Scanner Launcher
                val scannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                        if (scanResult != null) {
                            viewModel.handleScanSuccess(scanResult)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Unable to read scan result")
                            }
                        }
                    } else if (result.resultCode == Activity.RESULT_CANCELED) {
                        scope.launch {
                            snackbarHostState.showSnackbar(getString(R.string.scan_cancelled))
                        }
                    }
                }

                fun launchScanner() {
                    try {
                        val scannerOptions = GmsDocumentScannerOptions.Builder()
                            .setGalleryImportAllowed(true)
                            .setPageLimit(20)
                            .setResultFormats(
                                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                            )
                            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                            .build()
                        val scannerClient = GmsDocumentScanning.getClient(scannerOptions)
                        scannerClient.getStartScanIntent(this@MainActivity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                            .addOnFailureListener { e ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.scan_failed, e.localizedMessage ?: "Unknown error")
                                    )
                                }
                            }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        scope.launch {
                            snackbarHostState.showSnackbar("Document scanner unavailable: ${e.localizedMessage}")
                        }
                    }
                }

                // Gallery picker for "Import Gallery" tile
                val galleryPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    if (uri != null) {
                        // Open in OCR or Resizer
                        navController.navigate("ocr")
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is UiEvent.ShowSnackbar -> {
                                snackbarHostState.showSnackbar(event.message)
                            }
                            is UiEvent.NavigateToDocument -> {
                                // After a successful scan save, maybe show interstitial
                                viewModel.triggerInterstitialAfterPdfAction(this@MainActivity) {
                                    navController.navigate("document/${event.documentId}")
                                }
                            }
                        }
                    }
                }

                val showBottomNav = currentRoute in listOf("home", "library", "tools", "settings")

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = NeonDarkBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomNav) {
                            NavigationBar(
                                containerColor = Color(0xFF0B101D),
                                tonalElevation = 8.dp,
                                modifier = Modifier.testTag("main_bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "home",
                                    onClick = {
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = "Home",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0A0E17),
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyan,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "library",
                                    onClick = {
                                        navController.navigate("library") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = "Library",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Library", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0A0E17),
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyan,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "tools",
                                    onClick = {
                                        navController.navigate("tools") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.GridView,
                                            contentDescription = "Tools",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Tools", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0A0E17),
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyan,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0A0E17),
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = NeonCyan,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "onboarding",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                onGetStarted = {
                                    viewModel.completeOnboarding()
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                documents = documents,
                                onScanClick = { launchScanner() },
                                onImportGalleryClick = {
                                    galleryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onAiSummaryClick = { navController.navigate("ai_summary") },
                                onMyLibraryClick = { navController.navigate("library") },
                                onNavigateToImagesToPdf = { navController.navigate("images_to_pdf") },
                                onNavigateToResizer = { navController.navigate("resizer") },
                                onDocumentClick = { docId -> navController.navigate("document/$docId") },
                                onRenameDocument = { id, name -> viewModel.renameDocument(id, name) },
                                onDeleteDocument = { id -> viewModel.deleteDocument(id) },
                                onUnlockPro = { showProDialog = true },
                                onPdfSaved = { viewModel.triggerInterstitialAfterPdfAction(this@MainActivity) },
                                onShareComplete = { viewModel.triggerInterstitialAfterPdfAction(this@MainActivity) },
                                canRequestAds = canRequestAds,
                                proRemainingTime = proRemainingTime
                            )
                        }

                        composable("library") {
                            LibraryScreen(
                                documents = documents,
                                onScanClick = { launchScanner() },
                                onDocumentClick = { docId -> navController.navigate("document/$docId") },
                                onRenameDocument = { id, name -> viewModel.renameDocument(id, name) },
                                onDeleteDocument = { id -> viewModel.deleteDocument(id) },
                                onPdfSaved = { viewModel.triggerInterstitialAfterPdfAction(this@MainActivity) },
                                onShareComplete = { viewModel.triggerInterstitialAfterPdfAction(this@MainActivity) },
                                canRequestAds = canRequestAds
                            )
                        }

                        composable("tools") {
                            ToolsScreen(
                                onNavigateToTranslator = { navController.navigate("translator") },
                                onNavigateToOcr = { navController.navigate("ocr") },
                                onNavigateToAiSummary = { navController.navigate("ai_summary") },
                                onNavigateToImagesToPdf = { navController.navigate("images_to_pdf") },
                                onNavigateToResizer = { navController.navigate("resizer") }
                            )
                        }

                        composable("translator") {
                            TranslatorScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                isProUnlocked = isProUnlocked,
                                onUnlockPro = { showProDialog = true },
                                isPrivacyOptionsRequired = isPrivacyOptionsRequired,
                                onShowPrivacyOptions = {
                                    viewModel.consentManager.showPrivacyOptionsForm(this@MainActivity) {}
                                },
                                onOpenAdInspector = {
                                    com.example.ads.AdMobManager.openAdInspector(this@MainActivity) {}
                                },
                                canRequestAds = canRequestAds
                            )
                        }

                        composable(
                            route = "document/{documentId}",
                            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val docId = backStackEntry.arguments?.getString("documentId") ?: ""
                            DocumentScreen(
                                documentId = docId,
                                repository = viewModel.documentRepository,
                                onBack = { navController.popBackStack() },
                                onNavigateToOcr = { id -> navController.navigate("ocr?documentId=$id") },
                                onDeleteDocument = { id -> viewModel.deleteDocument(id) },
                                onRenameDocument = { id, name -> viewModel.renameDocument(id, name) },
                                onPdfSaved = { viewModel.triggerInterstitialAfterPdfAction(this@MainActivity) },
                                onShareComplete = { viewModel.triggerInterstitialAfterPdfAction(this@MainActivity) }
                            )
                        }

                        composable(
                            route = "export/{documentId}",
                            arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val docId = backStackEntry.arguments?.getString("documentId") ?: ""
                            ExportDocumentScreen(
                                documentId = docId,
                                repository = viewModel.documentRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "ocr?documentId={documentId}",
                            arguments = listOf(
                                navArgument("documentId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val docId = backStackEntry.arguments?.getString("documentId")
                            OcrScreen(
                                initialDocumentId = docId,
                                repository = viewModel.documentRepository,
                                isProUnlocked = isProUnlocked,
                                onUnlockPro = { showProDialog = true },
                                onWatchAdForPro = {
                                    viewModel.showRewardedAdForPro(this@MainActivity) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                isRewardedAdLoading = isRewardedLoading,
                                rewardedAdsWatchedCount = rewardedAdsWatchedCount,
                                onNavigateToAiSummary = { text ->
                                    navController.navigate("ai_summary?text=${Uri.encode(text)}")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "ai_summary?text={text}&documentId={documentId}",
                            arguments = listOf(
                                navArgument("text") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                                navArgument("documentId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val text = backStackEntry.arguments?.getString("text") ?: ""
                            val docId = backStackEntry.arguments?.getString("documentId")
                            AiSummaryScreen(
                                rawText = text,
                                documentId = docId,
                                repository = viewModel.documentRepository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("images_to_pdf") {
                            ImagesToPdfScreen(
                                onCreatePdf = { uris, config, onSuccess ->
                                    viewModel.createDocumentFromImages(uris, config, onSuccess)
                                },
                                onNavigateToDocument = { docId ->
                                    viewModel.triggerInterstitialAfterPdfAction(this@MainActivity) {
                                        navController.navigate("export/$docId") {
                                            popUpTo("home")
                                        }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("resizer") {
                            PhotoResizerScreen(
                                isProUnlocked = isProUnlocked,
                                onUnlockPro = { showProDialog = true },
                                onWatchAdForPro = {
                                    viewModel.showRewardedAdForPro(this@MainActivity) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                isRewardedAdLoading = isRewardedLoading,
                                rewardedAdsWatchedCount = rewardedAdsWatchedCount,
                                onBack = { navController.popBackStack() },
                                onPdfSaved = {
                                    viewModel.triggerInterstitialAfterPdfAction(this@MainActivity)
                                }
                            )
                        }
                    }

                    // First-Launch Age Verification Gate Dialog
                    AgeCheckDialog(
                        hasConfirmedAge = hasConfirmedAge,
                        onConfirmAdult = {
                            viewModel.confirmAge(true)
                        }
                    )

                    // Pro Access & Rewarded Ads Dialog
                    if (showProDialog) {
                        ProUnlockDialog(
                            isProUnlocked = isProUnlocked,
                            proRemainingTime = proRemainingTime,
                            adsWatchedCount = rewardedAdsWatchedCount,
                            onWatchAdForPro = {
                                viewModel.showRewardedAdForPro(this@MainActivity) { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                            onInstantUnlock = {
                                viewModel.unlockPro()
                            },
                            onDismissRequest = { showProDialog = false }
                        )
                    }
                }
            }
        }
    }
}
