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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DocumentRepository
import com.example.data.model.DocumentEntity
import com.example.engine.AiDocumentSummary
import com.example.engine.AiSummaryEngine
import com.example.engine.OcrEngine
import com.example.engine.OcrLanguage
import com.example.ui.theme.CyanButtonGradient
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkBackground
import com.example.ui.theme.NeonDarkBorder
import com.example.ui.theme.NeonDarkCardBackground
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSummaryScreen(
    rawText: String,
    documentId: String?,
    repository: DocumentRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeText by remember { mutableStateOf(rawText) }
    var summary by remember { mutableStateOf<AiDocumentSummary?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var pickedImageUri by remember { mutableStateOf<String?>(null) }
    var showLibraryPicker by remember { mutableStateOf(false) }

    val allDocuments by repository.allDocuments.collectAsState(initial = emptyList())

    fun processTextForSummary(text: String) {
        activeText = text
        summary = AiSummaryEngine.summarizeDocumentText(text)
    }

    LaunchedEffect(rawText, documentId) {
        if (rawText.isNotBlank()) {
            processTextForSummary(rawText)
        } else if (!documentId.isNullOrBlank()) {
            isAnalyzing = true
            val doc = repository.getDocumentById(documentId)
            if (doc != null) {
                val pagePaths = doc.getPagePaths()
                if (pagePaths.isNotEmpty()) {
                    val ocrResult = OcrEngine.recognizeTextFromImageFiles(
                        imagePaths = pagePaths,
                        language = OcrLanguage.AUTO_DETECT,
                        isPro = true
                    )
                    processTextForSummary(ocrResult.fullText.ifBlank { doc.name })
                } else {
                    processTextForSummary(doc.name)
                }
            }
            isAnalyzing = false
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedImageUri = uri.toString()
            scope.launch {
                isAnalyzing = true
                try {
                    val text = OcrEngine.recognizeTextFromUri(context, uri, OcrLanguage.AUTO_DETECT)
                    processTextForSummary(text.ifBlank { "Receipt / Document scanned from photo" })
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to analyze photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAnalyzing = false
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Document Summary",
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).testTag("ai_summary_back_button")
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
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(48.dp).testTag("ai_summary_pick_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Document Photo",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeonDarkBackground)
            )
        }
    ) { innerPadding ->
        val currentSummary = summary
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source Selector Bar: [Pick Photo] or [Select from Library]
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonDarkCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Select Document for AI Analysis",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("ai_summary_upload_btn")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Photo", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { showLibraryPicker = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("ai_summary_select_library_btn")
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("From Library", fontSize = 13.sp, color = TextWhite)
                            }
                        }
                    }
                }
            }

            if (isAnalyzing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13223E))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NeonCyan)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Analyzing document structure & key entities…", color = TextWhite, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Screen 7 Glowing Neon Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            2.dp,
                            Brush.verticalGradient(
                                listOf(NeonCyan, Color(0xFF1E88E5).copy(alpha = 0.5f))
                            ),
                            RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A30))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Summary",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                                if (currentSummary != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NeonCyan.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = currentSummary.detectedType,
                                            color = NeonCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Green check badge (Screen 7)
                            Surface(
                                shape = CircleShape,
                                color = NeonGreen,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF0A0E17),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (currentSummary != null) {
                            currentSummary.overviewBullets.forEach { bullet ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "✦",
                                        color = NeonGreen,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = bullet,
                                        color = TextWhite,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Key Points Section
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Key Points",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            currentSummary.keyPoints.forEach { kp ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = kp,
                                        color = TextMuted,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Upload a document photo or pick from library above to generate smart AI takeaways.",
                                color = TextMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Regenerate Action
                        Button(
                            onClick = {
                                if (activeText.isNotBlank()) {
                                    summary = AiSummaryEngine.summarizeDocumentText(activeText)
                                    Toast.makeText(context, "Summary refreshed", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please upload or select a document first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(CircleShape)
                                .background(CyanButtonGradient)
                                .testTag("regenerate_summary_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Regenerate",
                                    color = Color(0xFF0A0E17),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF0A0E17),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Copy & Share buttons
            if (currentSummary != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val fullSummary = buildString {
                                    appendLine("=== ${currentSummary.title} ===")
                                    appendLine("Type: ${currentSummary.detectedType}")
                                    appendLine()
                                    currentSummary.overviewBullets.forEach { b -> appendLine("• $b") }
                                    appendLine()
                                    appendLine("Key Points:")
                                    currentSummary.keyPoints.forEach { kp -> appendLine("• $kp") }
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Summary", fullSummary))
                                Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("copy_summary_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", color = TextWhite)
                        }

                        Button(
                            onClick = {
                                val fullSummary = buildString {
                                    appendLine("=== ${currentSummary.title} ===")
                                    appendLine("Type: ${currentSummary.detectedType}")
                                    appendLine()
                                    currentSummary.overviewBullets.forEach { b -> appendLine("• $b") }
                                    appendLine()
                                    appendLine("Key Points:")
                                    currentSummary.keyPoints.forEach { kp -> appendLine("• $kp") }
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, fullSummary)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share AI Summary"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("share_summary_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = TextWhite)
                        }
                    }
                }
            }
        }
    }

    // Pick from Library Dialog
    if (showLibraryPicker) {
        AlertDialog(
            onDismissRequest = { showLibraryPicker = false },
            title = { Text("Select Scanned Document", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                if (allDocuments.isEmpty()) {
                    Text("No documents found in library yet. Scan or upload one first.", color = TextMuted)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        items(allDocuments) { doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showLibraryPicker = false
                                        scope.launch {
                                            isAnalyzing = true
                                            val paths = doc.getPagePaths()
                                            if (paths.isNotEmpty()) {
                                                val ocrResult = OcrEngine.recognizeTextFromImageFiles(
                                                    imagePaths = paths,
                                                    language = OcrLanguage.AUTO_DETECT,
                                                    isPro = true
                                                )
                                                processTextForSummary(ocrResult.fullText.ifBlank { doc.name })
                                            } else {
                                                processTextForSummary(doc.name)
                                            }
                                            isAnalyzing = false
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(doc.name, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${doc.pageCount} pages", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLibraryPicker = false }) {
                    Text("Close", color = NeonCyan)
                }
            },
            containerColor = NeonDarkCardBackground
        )
    }
}
