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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.engine.OcrEngine
import com.example.engine.OcrLanguage
import com.example.engine.SupportedLanguage
import com.example.engine.TranslationEngine
import com.example.ui.theme.CyanButtonGradient
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkBackground
import com.example.ui.theme.NeonDarkBorder
import com.example.ui.theme.NeonDarkCardBackground
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    initialText: String = "",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceText by rememberSaveable { mutableStateOf(initialText) }
    var translatedText by rememberSaveable { mutableStateOf("") }
    var pickedImageUri by rememberSaveable { mutableStateOf<String?>(null) }

    var detectedLanguageCode by rememberSaveable { mutableStateOf("en") }
    var detectedLanguageName by rememberSaveable { mutableStateOf("Auto Detecting...") }
    var selectedTargetLanguage by remember { mutableStateOf(TranslationEngine.targetLanguages[1]) } // Spanish default

    var isOcrRunning by rememberSaveable { mutableStateOf(false) }
    var isTranslating by rememberSaveable { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedImageUri = uri.toString()
            scope.launch {
                isOcrRunning = true
                try {
                    val extracted = OcrEngine.recognizeTextFromUri(context, uri, OcrLanguage.AUTO_DETECT)
                    sourceText = extracted
                    if (extracted.isNotBlank()) {
                        val (code, name) = OcrEngine.identifyLanguage(extracted)
                        detectedLanguageCode = code
                        detectedLanguageName = name

                        // Auto-select opposite target language if source matches default target
                        if (code.equals(selectedTargetLanguage.code, ignoreCase = true)) {
                            selectedTargetLanguage = if (code.equals("en", ignoreCase = true)) {
                                TranslationEngine.targetLanguages.first { it.code == "es" }
                            } else {
                                TranslationEngine.targetLanguages.first { it.code == "en" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to read text from image", Toast.LENGTH_SHORT).show()
                } finally {
                    isOcrRunning = false
                }
            }
        }
    }

    // Save as .txt SAF launcher
    val saveTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null && translatedText.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(translatedText.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Translation saved to device", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun executeTranslation() {
        if (sourceText.isBlank()) {
            Toast.makeText(context, "Please upload a photo or type text to translate", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isTranslating = true
            try {
                // Confirm detected language
                val (detectedCode, detectedName) = OcrEngine.identifyLanguage(sourceText)
                detectedLanguageCode = detectedCode
                detectedLanguageName = detectedName

                val result = TranslationEngine.translateText(
                    text = sourceText,
                    sourceLanguageCode = detectedCode,
                    targetLanguageCode = selectedTargetLanguage.code
                )

                translatedText = result.getOrDefault(sourceText)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Translation error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isTranslating = false
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
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Photo & Text Translator",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).testTag("translator_back_button")
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
                        modifier = Modifier.size(48.dp).testTag("translator_pick_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Photo",
                            tint = NeonCyan
                        )
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
            // Upload Photo / Preview Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonDarkCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Photo Source",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                modifier = Modifier.height(38.dp).testTag("translator_upload_photo_btn")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (pickedImageUri != null) "Change Photo" else "Upload Photo", fontSize = 13.sp)
                            }
                        }

                        if (pickedImageUri != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = Uri.parse(pickedImageUri),
                                    contentDescription = "Uploaded document photo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (isOcrRunning) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NeonCyan)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Reading text and detecting language…", color = NeonCyan, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Language Bar: [Detected Language (Auto)]  ➔  [Select Target Language]
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101C36)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Auto-Detected Language
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto Detected", fontSize = 11.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = detectedLanguageName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Translates to",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )

                        // Right: Target Language Selector (Clickable)
                        Surface(
                            onClick = { showLanguagePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1A2A4E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.padding(start = 12.dp).testTag("select_target_language_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedTargetLanguage.flag} ${selectedTargetLanguage.name.substringBefore(" ")}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Original Text Box (Editable / Selectable)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NeonDarkCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonDarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Original Text",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            if (sourceText.isNotBlank()) {
                                Text(
                                    text = "${sourceText.length} chars",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = sourceText,
                            onValueChange = {
                                sourceText = it
                                scope.launch {
                                    val (code, name) = OcrEngine.identifyLanguage(it)
                                    detectedLanguageCode = code
                                    detectedLanguageName = name
                                }
                            },
                            placeholder = { Text("Upload a photo or paste text to translate...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth().height(130.dp).testTag("translator_input_text"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }
            }

            // Translate Action Button
            item {
                Button(
                    onClick = { executeTranslation() },
                    enabled = sourceText.isNotBlank() && !isTranslating,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(CyanButtonGradient)
                        .testTag("translator_action_button")
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF0A0E17))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Translating On-Device…", color = Color(0xFF0A0E17), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = Color(0xFF0A0E17), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Translate to ${selectedTargetLanguage.name.substringBefore(" ")}", color = Color(0xFF0A0E17), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Translated Result Card
            if (translatedText.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedTargetLanguage.flag, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Translated Text (${selectedTargetLanguage.name.substringBefore(" ")})",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE0F2FE)
                                ) {
                                    Text(
                                        text = selectedTargetLanguage.code.uppercase(),
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            SelectionContainer {
                                Text(
                                    text = translatedText,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.testTag("translated_result_text")
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons: Copy, Share, Save as .txt
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Translated Text", translatedText))
                                        Toast.makeText(context, "Translation copied", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("copy_translation_btn")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", color = Color.White, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, translatedText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Translation"))
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("share_translation_btn")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", color = Color.White, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        saveTxtLauncher.launch("AuraScan_Translated_${selectedTargetLanguage.code}.txt")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("save_translation_btn")
                                ) {
                                    Icon(Icons.Default.SaveAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Target Language Picker Dialog (30+ Languages with Flags)
    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = {
                Text("Select Target Language", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    items(TranslationEngine.targetLanguages.size) { index ->
                        val lang = TranslationEngine.targetLanguages[index]
                        val isSelected = lang.code == selectedTargetLanguage.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1E3A8A) else Color.Transparent)
                                .clickable {
                                    selectedTargetLanguage = lang
                                    showLanguagePicker = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = lang.name,
                                color = if (isSelected) NeonCyan else TextWhite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text("Close", color = NeonCyan)
                }
            },
            containerColor = NeonDarkCardBackground
        )
    }
}
