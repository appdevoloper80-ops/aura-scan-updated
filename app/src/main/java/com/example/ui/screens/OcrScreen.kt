package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DocumentRepository
import com.example.data.model.DocumentEntity
import com.example.engine.OcrEngine
import com.example.engine.OcrLanguage
import com.example.ui.theme.CyanButtonGradient
import com.example.ui.theme.DarkButtonGradient
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkBackground
import com.example.ui.theme.NeonDarkBorder
import com.example.ui.theme.NeonDarkCardBackground
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.Constants
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    initialDocumentId: String?,
    repository: DocumentRepository,
    isProUnlocked: Boolean,
    onUnlockPro: () -> Unit,
    onWatchAdForPro: () -> Unit = onUnlockPro,
    isRewardedAdLoading: Boolean = false,
    rewardedAdsWatchedCount: Int = 0,
    onNavigateToAiSummary: (String) -> Unit,
    onNavigateToTranslator: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedLanguage by rememberSaveable { mutableStateOf(OcrLanguage.AUTO_DETECT) }
    var extractedText by rememberSaveable { mutableStateOf("") }
    var detectedLanguageName by rememberSaveable { mutableStateOf("Auto-detecting…") }
    var isProcessing by rememberSaveable { mutableStateOf(false) }
    var progressCurrent by rememberSaveable { mutableIntStateOf(0) }
    var progressTotal by rememberSaveable { mutableIntStateOf(0) }
    var isLimitApplied by rememberSaveable { mutableStateOf(false) }
    var pickedImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var activeDocument by remember { mutableStateOf<DocumentEntity?>(null) }

    fun runOcr(lang: OcrLanguage) {
        val doc = activeDocument
        val uriStr = pickedImageUri
        if (doc != null) {
            scope.launch {
                isProcessing = true
                try {
                    val paths = doc.getPagePaths()
                    progressTotal = paths.size
                    val result = OcrEngine.recognizeTextFromImageFiles(
                        imagePaths = paths,
                        language = lang,
                        isPro = isProUnlocked,
                        freeLimit = Constants.FREE_OCR_PAGE_LIMIT,
                        onProgress = { cur, tot ->
                            progressCurrent = cur
                            progressTotal = tot
                        }
                    )
                    extractedText = result.fullText
                    isLimitApplied = result.isLimitApplied
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "OCR Error: ${e.localizedMessage ?: "Failed to process image"}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        } else if (uriStr != null) {
            val uri = Uri.parse(uriStr)
            scope.launch {
                isProcessing = true
                try {
                    val result = OcrEngine.recognizeTextFromUri(context, uri, lang)
                    extractedText = result
                    isLimitApplied = false
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "OCR Error: ${e.localizedMessage ?: "Failed to process image"}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    LaunchedEffect(extractedText) {
        if (extractedText.isNotBlank()) {
            val (code, name) = OcrEngine.identifyLanguage(extractedText)
            detectedLanguageName = if (code != "und") "$name ($code)" else "Auto-detected"
        }
    }

    LaunchedEffect(initialDocumentId) {
        if (!initialDocumentId.isNullOrBlank()) {
            activeDocument = repository.getDocumentById(initialDocumentId)
            // Trigger OCR immediately for initial document
            activeDocument?.let { runOcr(selectedLanguage) }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedImageUri = uri.toString()
            activeDocument = null
            scope.launch {
                isProcessing = true
                try {
                    val result = OcrEngine.recognizeTextFromUri(context, uri, selectedLanguage)
                    extractedText = result
                    isLimitApplied = false
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "OCR Error: ${e.localizedMessage ?: "Failed to process image"}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NeonDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OCR Result",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).testTag("ocr_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button),
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (extractedText.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AuraScan OCR", extractedText))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(48.dp).testTag("ocr_action_copy")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextWhite)
                    }

                    IconButton(
                        onClick = {
                            if (extractedText.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, extractedText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share OCR Text"))
                            }
                        },
                        modifier = Modifier.size(48.dp).testTag("ocr_action_share")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeonDarkBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language Selection Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OcrLanguage.values().forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = {
                                selectedLanguage = lang
                                runOcr(lang)
                            },
                            label = {
                                Text(
                                    lang.displayName,
                                    color = if (selectedLanguage == lang) Color(0xFF0A0E17) else TextWhite,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                containerColor = Color(0xFF131D33)
                            ),
                            shape = CircleShape
                        )
                    }

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pick Image", tint = NeonCyan)
                    }
                }
            }

            if (isProcessing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13223E))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = NeonCyan)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Extracting on-device text…", color = TextWhite, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Free OCR limit reached card (Pro Rewarded Ad)
            if (isLimitApplied && !isProUnlocked) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ocr_pro_limit_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131F37)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Free limit: First 3 pages extracted",
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Unlock unlimited pages and full extraction speed for 5 minutes by watching 5 short video ads.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onWatchAdForPro,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("ocr_watch_ad_button")
                            ) {
                                if (isRewardedAdLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color(0xFF0A0E17),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading ad…", color = Color(0xFF0A0E17), fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF0A0E17)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Watch Ad ($rewardedAdsWatchedCount/5) to Unlock Pro",
                                        color = Color(0xFF0A0E17),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // High Contrast White Card matching Screen 6 (OCR Result)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // "T Extracted Text" badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1976D2),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("T", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Extracted Text",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Text Body
                        if (extractedText.isBlank()) {
                            Text(
                                text = if (isProcessing) "Reading image text..." else "No text detected in selected image.",
                                color = Color(0xFF64748B),
                                fontSize = 15.sp,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            SelectionContainer {
                                Text(
                                    text = extractedText,
                                    color = Color(0xFF0F172A),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.testTag("ocr_text_output")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Languages Detected Footer inside White Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color(0xFF1E88E5),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (extractedText.isNotBlank()) "Languages Detected: $detectedLanguageName" else "OCR Engine: ${selectedLanguage.displayName}",
                                    color = Color(0xFF334155),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Buttons (Screen 6): "Copy Text" and "Share"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Copy Text button (Dark pill)
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AuraScan OCR", extractedText))
                            Toast.makeText(context, "Text copied", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("ocr_copy_text_button")
                    ) {
                        Text("Copy Text", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share button (Cyan pill)
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, extractedText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share OCR Text"))
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(CyanButtonGradient)
                            .testTag("ocr_share_text_button")
                    ) {
                        Text("Share", color = Color(0xFF0A0E17), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Translate Text & AI Document Summary actions
            if (extractedText.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onNavigateToTranslator(extractedText) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A2640)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("ocr_open_translator_button")
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Translate Text (Auto-detect) 🌐", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { onNavigateToAiSummary(extractedText) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10284E)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("ocr_open_ai_summary_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Document Summary ✨", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
