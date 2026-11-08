package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class OcrLanguage(val displayName: String) {
    AUTO_DETECT("Auto Detect"),
    LATIN("English / Latin"),
    DEVANAGARI("Hindi (Devanagari)"),
    CHINESE("Chinese (中文)"),
    JAPANESE("Japanese (日本語)"),
    KOREAN("Korean (한국어)")
}

data class OcrResult(
    val fullText: String,
    val processedPages: Int,
    val totalPages: Int,
    val isLimitApplied: Boolean,
    val detectedLanguageCode: String = "und",
    val detectedLanguageName: String = "Unknown"
)

object OcrEngine {

    fun getRecognizer(language: OcrLanguage): TextRecognizer {
        return when (language) {
            OcrLanguage.AUTO_DETECT, OcrLanguage.LATIN ->
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            OcrLanguage.DEVANAGARI ->
                TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            OcrLanguage.CHINESE ->
                TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            OcrLanguage.JAPANESE ->
                TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            OcrLanguage.KOREAN ->
                TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        }
    }

    suspend fun recognizeTextFromBitmap(
        bitmap: Bitmap,
        language: OcrLanguage = OcrLanguage.AUTO_DETECT
    ): String = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizeWithLanguageStrategy(image, language)
    }

    suspend fun recognizeTextFromUri(
        context: Context,
        uri: Uri,
        language: OcrLanguage = OcrLanguage.AUTO_DETECT
    ): String = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, uri)
        recognizeWithLanguageStrategy(image, language)
    }

    private suspend fun recognizeWithLanguageStrategy(
        image: InputImage,
        language: OcrLanguage
    ): String {
        if (language != OcrLanguage.AUTO_DETECT) {
            val recognizer = getRecognizer(language)
            return processImage(recognizer, image)
        }

        // Auto Detect strategy:
        // 1. Try standard Latin
        val latinRecognizer = getRecognizer(OcrLanguage.LATIN)
        val latinText = processImage(latinRecognizer, image)
        if (latinText.isNotBlank() && containsAlphanumeric(latinText)) {
            return latinText
        }

        // 2. Try Devanagari (Hindi)
        val devRecognizer = getRecognizer(OcrLanguage.DEVANAGARI)
        val devText = processImage(devRecognizer, image)
        if (devText.isNotBlank()) {
            return devText
        }

        // 3. Try Chinese
        val zhRecognizer = getRecognizer(OcrLanguage.CHINESE)
        val zhText = processImage(zhRecognizer, image)
        if (zhText.isNotBlank()) {
            return zhText
        }

        // 4. Try Japanese
        val jaRecognizer = getRecognizer(OcrLanguage.JAPANESE)
        val jaText = processImage(jaRecognizer, image)
        if (jaText.isNotBlank() && jaText.any { it.code in 0x3040..0x30FF || it.code in 0x4E00..0x9FFF }) {
            return jaText
        }

        // 5. Try Korean
        val koRecognizer = getRecognizer(OcrLanguage.KOREAN)
        val koText = processImage(koRecognizer, image)
        if (koText.isNotBlank() && koText.any { it.code in 0xAC00..0xD7AF }) {
            return koText
        }

        return latinText
    }

    private fun containsAlphanumeric(text: String): Boolean {
        return text.any { it.isLetterOrDigit() }
    }

    suspend fun recognizeTextFromImageFiles(
        imagePaths: List<String>,
        language: OcrLanguage,
        isPro: Boolean,
        freeLimit: Int = 3,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): OcrResult = withContext(Dispatchers.IO) {
        val total = imagePaths.size
        val pagesToProcess = if (isPro || total <= freeLimit) total else freeLimit
        val isLimitApplied = !isPro && total > freeLimit

        val sb = StringBuilder()

        for (i in 0 until pagesToProcess) {
            onProgress(i + 1, pagesToProcess)
            val path = imagePaths[i]
            val file = File(path)
            if (file.exists()) {
                val bitmap = decodeSampledBitmap(file.absolutePath, 1600, 1600)
                if (bitmap != null) {
                    try {
                        val inputImage = InputImage.fromBitmap(bitmap, 0)
                        val text = recognizeWithLanguageStrategy(inputImage, language)
                        if (text.isNotBlank()) {
                            if (pagesToProcess > 1) {
                                sb.append("--- Page ").append(i + 1).append(" ---\n")
                            }
                            sb.append(text).append("\n\n")
                        }
                    } finally {
                        if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                    }
                }
            }
        }

        val resultText = sb.toString().trim()
        val (langCode, langName) = identifyLanguage(resultText)

        OcrResult(
            fullText = resultText,
            processedPages = pagesToProcess,
            totalPages = total,
            isLimitApplied = isLimitApplied,
            detectedLanguageCode = langCode,
            detectedLanguageName = langName
        )
    }

    /**
     * Identify the language of the extracted text using ML Kit on-device LanguageIdentification.
     */
    suspend fun identifyLanguage(text: String): Pair<String, String> = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext Pair("und", "Unknown")

        try {
            val languageIdentifier = LanguageIdentification.getClient()
            val languageCode = suspendCancellableCoroutine<String> { continuation ->
                languageIdentifier.identifyLanguage(text)
                    .addOnSuccessListener { code -> continuation.resume(code) }
                    .addOnFailureListener { continuation.resume("und") }
            }

            if (languageCode == "und" || languageCode.isBlank()) {
                // Heuristic check for common script-specific ranges
                val hasDevanagari = text.any { it.code in 0x0900..0x097F }
                val hasChinese = text.any { it.code in 0x4E00..0x9FFF }
                val hasJapanese = text.any { it.code in 0x3040..0x309F || it.code in 0x30A0..0x30FF }
                val hasKorean = text.any { it.code in 0xAC00..0xD7AF }
                val hasArabic = text.any { it.code in 0x0600..0x06FF }
                val hasCyrillic = text.any { it.code in 0x0400..0x04FF }

                when {
                    hasKorean -> Pair("ko", "Korean")
                    hasJapanese -> Pair("ja", "Japanese")
                    hasChinese -> Pair("zh", "Chinese")
                    hasDevanagari -> Pair("hi", "Hindi")
                    hasArabic -> Pair("ar", "Arabic")
                    hasCyrillic -> Pair("ru", "Russian")
                    else -> Pair("en", "English")
                }
            } else {
                val locale = Locale.forLanguageTag(languageCode)
                val displayName = locale.getDisplayLanguage(Locale.ENGLISH).ifBlank {
                    when (languageCode) {
                        "zh" -> "Chinese"
                        "hi" -> "Hindi"
                        "ja" -> "Japanese"
                        "ko" -> "Korean"
                        else -> languageCode.uppercase()
                    }
                }
                Pair(languageCode, displayName)
            }
        } catch (e: Exception) {
            Pair("und", "Unknown")
        }
    }

    private suspend fun processImage(recognizer: TextRecognizer, image: InputImage): String {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
    }

    fun decodeSampledBitmap(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(filePath, options)
    }

    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
