package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.engine.OcrEngine
import com.example.engine.ResizerConfig
import com.example.engine.ResizerEngine
import com.example.engine.ResizerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ResizerPreset {
    EXAM_PHOTO,
    EXAM_SIGNATURE,
    PASSPORT_PHOTO,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoResizerScreen(
    isProUnlocked: Boolean,
    onUnlockPro: () -> Unit,
    onWatchAdForPro: () -> Unit = onUnlockPro,
    isRewardedAdLoading: Boolean = false,
    rewardedAdsWatchedCount: Int = 0,
    onBack: () -> Unit,
    onPdfSaved: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pickedImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPreset by rememberSaveable { mutableStateOf(ResizerPreset.EXAM_PHOTO) }

    // Custom configuration parameters
    var customWidthStr by rememberSaveable { mutableStateOf("400") }
    var customHeightStr by rememberSaveable { mutableStateOf("400") }
    var customMaxKbStr by rememberSaveable { mutableStateOf("100") }
    var keepAspectRatio by rememberSaveable { mutableStateOf(true) }

    // Crop dialog state
    var showCropDialog by rememberSaveable { mutableStateOf(false) }
    var cropZoomFactor by rememberSaveable { mutableFloatStateOf(1f) } // 1.0 (full) to 0.5 (close crop)
    var cropAspectPreset by rememberSaveable { mutableStateOf("1:1") }

    // Pro gating dialog
    var showProDialog by rememberSaveable { mutableStateOf(false) }

    // Processing & Result states
    var isOptimizing by rememberSaveable { mutableStateOf(false) }
    var resizerResult by remember { mutableStateOf<ResizerResult?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedImageUri = uri.toString()
            resizerResult = null
            resultBitmap = null
        }
    }

    // Save image SAF launcher
    val saveImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri: Uri? ->
        val bytes = resizerResult?.outputBytes
        if (uri != null && bytes != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                }
                Toast.makeText(context, context.getString(R.string.image_saved_success), Toast.LENGTH_SHORT).show()
                onPdfSaved() // Trigger interstitial logic in MainActivity
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun executeResizing(croppedSourceBitmap: Bitmap? = null) {
        val uriStr = pickedImageUri ?: return

        // Check Pro gates: Custom preset or max KB < 20
        val targetMaxKb = when (selectedPreset) {
            ResizerPreset.EXAM_PHOTO -> 50
            ResizerPreset.EXAM_SIGNATURE -> 20
            ResizerPreset.PASSPORT_PHOTO -> 200
            ResizerPreset.CUSTOM -> customMaxKbStr.toIntOrNull() ?: 100
        }

        if ((selectedPreset == ResizerPreset.CUSTOM || targetMaxKb < 20) && !isProUnlocked) {
            showProDialog = true
            return
        }

        scope.launch {
            isOptimizing = true
            try {
                val inputBitmap: Bitmap? = croppedSourceBitmap ?: withContext(Dispatchers.IO) {
                    OcrEngine.decodeSampledBitmapFromUri(context, Uri.parse(uriStr), 2000, 2000)
                }

                if (inputBitmap == null) {
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                    isOptimizing = false
                    return@launch
                }

                val config = when (selectedPreset) {
                    ResizerPreset.EXAM_PHOTO -> ResizerConfig(
                        targetWidth = 200,
                        targetHeight = 230,
                        maxFileSizeBytes = 50 * 1024L,
                        keepAspectRatio = true
                    )
                    ResizerPreset.EXAM_SIGNATURE -> ResizerConfig(
                        targetWidth = 140,
                        targetHeight = 60,
                        maxFileSizeBytes = 20 * 1024L,
                        keepAspectRatio = false
                    )
                    ResizerPreset.PASSPORT_PHOTO -> ResizerConfig(
                        targetWidth = 413,
                        targetHeight = 531,
                        maxFileSizeBytes = 200 * 1024L,
                        keepAspectRatio = true
                    )
                    ResizerPreset.CUSTOM -> {
                        val w = customWidthStr.toIntOrNull()?.coerceAtLeast(20) ?: 400
                        val h = customHeightStr.toIntOrNull()?.coerceAtLeast(20) ?: 400
                        val kb = customMaxKbStr.toLongOrNull()?.coerceAtLeast(5) ?: 100L
                        ResizerConfig(
                            targetWidth = w,
                            targetHeight = h,
                            maxFileSizeBytes = kb * 1024L,
                            keepAspectRatio = keepAspectRatio
                        )
                    }
                }

                val result = withContext(Dispatchers.Default) {
                    ResizerEngine.compressToTarget(inputBitmap, config)
                }

                val bmp = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(result.outputBytes, 0, result.outputBytes.size)
                }

                resizerResult = result
                resultBitmap = bmp

                if (croppedSourceBitmap == null && !inputBitmap.isRecycled) {
                    inputBitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Optimization error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isOptimizing = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = com.example.ui.theme.NeonDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.resizer_title),
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("resizer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button),
                            tint = com.example.ui.theme.TextWhite
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
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("resizer_pick_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = stringResource(R.string.select_photo_button),
                            tint = com.example.ui.theme.NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.example.ui.theme.NeonDarkBackground)
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
            // Selected Image Preview or Prompt
            item {
                if (pickedImageUri == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.resizer_empty_prompt),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp).testTag("resizer_pick_initial_button")
                            ) {
                                Text(stringResource(R.string.select_photo_button))
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selected Photo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                TextButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Text("Change")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = Uri.parse(pickedImageUri),
                                    contentDescription = "Selected source",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Presets Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.preset_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PresetRadioButton(
                                title = stringResource(R.string.preset_exam_photo),
                                subtitle = "200×230 px • Max 50 KB • Free",
                                selected = selectedPreset == ResizerPreset.EXAM_PHOTO,
                                isPro = false,
                                onClick = { selectedPreset = ResizerPreset.EXAM_PHOTO }
                            )

                            PresetRadioButton(
                                title = stringResource(R.string.preset_exam_sign),
                                subtitle = "140×60 px • Max 20 KB • Free",
                                selected = selectedPreset == ResizerPreset.EXAM_SIGNATURE,
                                isPro = false,
                                onClick = { selectedPreset = ResizerPreset.EXAM_SIGNATURE }
                            )

                            PresetRadioButton(
                                title = stringResource(R.string.preset_passport),
                                subtitle = "413×531 px (35×45 mm) • Free",
                                selected = selectedPreset == ResizerPreset.PASSPORT_PHOTO,
                                isPro = false,
                                onClick = { selectedPreset = ResizerPreset.PASSPORT_PHOTO }
                            )

                            PresetRadioButton(
                                title = stringResource(R.string.preset_custom),
                                subtitle = "Custom pixels & file size • Pro Feature",
                                selected = selectedPreset == ResizerPreset.CUSTOM,
                                isPro = !isProUnlocked,
                                onClick = {
                                    if (!isProUnlocked) {
                                        showProDialog = true
                                    } else {
                                        selectedPreset = ResizerPreset.CUSTOM
                                    }
                                }
                            )
                        }

                        // Custom Mode Fields
                        AnimatedVisibility(visible = selectedPreset == ResizerPreset.CUSTOM) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customWidthStr,
                                        onValueChange = { customWidthStr = it },
                                        label = { Text(stringResource(R.string.width_px)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("custom_width_input")
                                    )
                                    OutlinedTextField(
                                        value = customHeightStr,
                                        onValueChange = { customHeightStr = it },
                                        label = { Text(stringResource(R.string.height_px)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f).testTag("custom_height_input")
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = customMaxKbStr,
                                    onValueChange = { customMaxKbStr = it },
                                    label = { Text(stringResource(R.string.max_size_kb)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("custom_kb_input")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.keep_aspect_ratio),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Switch(
                                        checked = keepAspectRatio,
                                        onCheckedChange = { keepAspectRatio = it },
                                        modifier = Modifier.testTag("switch_aspect_ratio")
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Simple Crop Step Button
                                OutlinedButton(
                                    onClick = { showCropDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("custom_crop_button")
                                ) {
                                    Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simple Crop Selection")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Button
                        Button(
                            onClick = { executeResizing() },
                            enabled = pickedImageUri != null && !isOptimizing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("run_resizer_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isOptimizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.resizing_progress))
                            } else {
                                Text(stringResource(R.string.resize_button))
                            }
                        }
                    }
                }
            }

            // Results Card
            val result = resizerResult
            val bmp = resultBitmap
            if (result != null && bmp != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.result_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val sizeKb = result.finalSizeBytes / 1024f
                            Text(
                                text = stringResource(
                                    R.string.compressed_label,
                                    "%.1f KB".format(sizeKb),
                                    result.outputWidth,
                                    result.outputHeight
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "JPEG Quality: ${result.finalQuality}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Optimized result",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        shareOptimizedImage(context, result.outputBytes)
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("share_resized_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.share_image_button))
                                }

                                Button(
                                    onClick = {
                                        saveImageLauncher.launch("AuraScan_${System.currentTimeMillis()}.jpg")
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("save_resized_button")
                                ) {
                                    Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.save_image_button))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Simple Crop Dialog
    if (showCropDialog && pickedImageUri != null) {
        AlertDialog(
            onDismissRequest = { showCropDialog = false },
            title = { Text(stringResource(R.string.crop_step_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Adjust crop framing zoom",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = Uri.parse(pickedImageUri),
                            contentDescription = "Crop preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Framing Zoom: ${(cropZoomFactor * 100).toInt()}%")
                    Slider(
                        value = cropZoomFactor,
                        onValueChange = { cropZoomFactor = it },
                        valueRange = 0.5f..1.0f,
                        modifier = Modifier.fillMaxWidth().testTag("crop_slider")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCropDialog = false
                        // Perform center crop with zoom factor
                        scope.launch {
                            val uri = Uri.parse(pickedImageUri)
                            val original = withContext(Dispatchers.IO) {
                                OcrEngine.decodeSampledBitmapFromUri(context, uri, 1800, 1800)
                            }
                            if (original != null) {
                                val cropW = (original.width * cropZoomFactor).roundToInt()
                                val cropH = (original.height * cropZoomFactor).roundToInt()
                                val left = max(0, (original.width - cropW) / 2)
                                val top = max(0, (original.height - cropH) / 2)
                                val cropped = Bitmap.createBitmap(original, left, top, cropW, cropH)
                                executeResizing(cropped)
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.crop_step_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCropDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // Pro Gate Dialog
    if (showProDialog) {
        AlertDialog(
            onDismissRequest = { showProDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pro_gated_title))
                }
            },
            text = {
                Text(stringResource(R.string.pro_gated_desc))
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onWatchAdForPro,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resizer_watch_ad_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.NeonCyan)
                    ) {
                        if (isRewardedAdLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF0A0E17))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading ad…", color = Color(0xFF0A0E17))
                        } else {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFF0A0E17))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Watch Ad ($rewardedAdsWatchedCount/5) for 5min Pro", color = Color(0xFF0A0E17), fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            onUnlockPro()
                            showProDialog = false
                            Toast.makeText(context, context.getString(R.string.pro_unlocked_toast), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_unlock_pro_button")
                    ) {
                        Text(stringResource(R.string.pro_gated_button))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showProDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun PresetRadioButton(
    title: String,
    subtitle: String,
    selected: Boolean,
    isPro: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                    )
                    if (isPro) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Pro",
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

fun shareOptimizedImage(context: Context, bytes: ByteArray) {
    try {
        val tempFile = File(context.cacheDir, "AuraScan_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { it.write(bytes) }
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, tempFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Image"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
