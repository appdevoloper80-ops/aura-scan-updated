package com.example.engine

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ResizerConfig(
    val targetWidth: Int,
    val targetHeight: Int,
    val maxFileSizeBytes: Long,
    val keepAspectRatio: Boolean = true
)

data class ResizerResult(
    val outputBytes: ByteArray,
    val outputWidth: Int,
    val outputHeight: Int,
    val finalQuality: Int,
    val finalSizeBytes: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResizerResult

        if (!outputBytes.contentEquals(other.outputBytes)) return false
        if (outputWidth != other.outputWidth) return false
        if (outputHeight != other.outputHeight) return false
        if (finalQuality != other.finalQuality) return false
        if (finalSizeBytes != other.finalSizeBytes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = outputBytes.contentHashCode()
        result = 31 * result + outputWidth
        result = 31 * result + outputHeight
        result = 31 * result + finalQuality
        result = 31 * result + finalSizeBytes.hashCode()
        return result
    }
}

object ResizerEngine {

    /**
     * Pure Kotlin function to calculate scaled target dimensions.
     */
    fun calculateTargetDimensions(
        inputWidth: Int,
        inputHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        keepAspectRatio: Boolean
    ): Pair<Int, Int> {
        require(inputWidth > 0 && inputHeight > 0) { "Input dimensions must be > 0" }
        require(targetWidth > 0 && targetHeight > 0) { "Target dimensions must be > 0" }

        return if (keepAspectRatio) {
            val scale = min(
                targetWidth.toFloat() / inputWidth.toFloat(),
                targetHeight.toFloat() / inputHeight.toFloat()
            )
            val w = max(1, (inputWidth * scale).roundToInt())
            val h = max(1, (inputHeight * scale).roundToInt())
            Pair(w, h)
        } else {
            Pair(targetWidth, targetHeight)
        }
    }

    /**
     * Pure Kotlin binary search for optimal JPEG quality (between minQuality and maxQuality).
     * Finds highest quality where size <= maxFileSizeBytes.
     */
    fun binarySearchQuality(
        maxFileSizeBytes: Long,
        minQuality: Int = 10,
        maxQuality: Int = 95,
        getSizeForQuality: (quality: Int) -> Long
    ): Int? {
        var low = minQuality
        var high = maxQuality
        var bestQuality: Int? = null

        while (low <= high) {
            val mid = (low + high) / 2
            val size = getSizeForQuality(mid)
            if (size <= maxFileSizeBytes) {
                bestQuality = mid
                low = mid + 1 // Attempt higher visual quality
            } else {
                high = mid - 1 // Too large, reduce quality
            }
        }

        return bestQuality
    }

    /**
     * Pure Kotlin calculation of next step downscale dimension (5% reduction).
     */
    fun calculateNextDownscale(currentWidth: Int, currentHeight: Int): Pair<Int, Int> {
        val nextW = (currentWidth * 0.95f).roundToInt()
        val nextH = (currentHeight * 0.95f).roundToInt()
        return Pair(nextW, nextH)
    }

    /**
     * High-level Android compression function connecting pure algorithms to Bitmap operations.
     */
    fun compressToTarget(
        inputBitmap: Bitmap,
        config: ResizerConfig,
        minDimension: Int = 16
    ): ResizerResult {
        require(config.targetWidth > 0 && config.targetHeight > 0) { "Target dimensions must be > 0" }
        require(config.maxFileSizeBytes > 0) { "Max file size must be > 0" }

        val (initialW, initialH) = calculateTargetDimensions(
            inputBitmap.width,
            inputBitmap.height,
            config.targetWidth,
            config.targetHeight,
            config.keepAspectRatio
        )

        var currentW = initialW
        var currentH = initialH
        var lastAttemptBytes: ByteArray? = null
        var lastAttemptQuality = 10
        var lastAttemptW = currentW
        var lastAttemptH = currentH

        while (currentW >= minDimension && currentH >= minDimension) {
            val scaledBitmap = if (currentW == inputBitmap.width && currentH == inputBitmap.height) {
                inputBitmap
            } else {
                Bitmap.createScaledBitmap(inputBitmap, currentW, currentH, true)
            }

            var low = 10
            var high = 95
            var bestQualityForDimension: Int? = null
            var bestBytesForDimension: ByteArray? = null

            while (low <= high) {
                val mid = (low + high) / 2
                val bytes = compressJpeg(scaledBitmap, mid)
                if (bytes.size <= config.maxFileSizeBytes) {
                    bestQualityForDimension = mid
                    bestBytesForDimension = bytes
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }

            if (scaledBitmap !== inputBitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }

            if (bestBytesForDimension != null) {
                return ResizerResult(
                    outputBytes = bestBytesForDimension,
                    outputWidth = currentW,
                    outputHeight = currentH,
                    finalQuality = bestQualityForDimension ?: 10,
                    finalSizeBytes = bestBytesForDimension.size.toLong()
                )
            }

            // Fallback attempt
            val fallbackScaled = if (currentW == inputBitmap.width && currentH == inputBitmap.height) {
                inputBitmap
            } else {
                Bitmap.createScaledBitmap(inputBitmap, currentW, currentH, true)
            }
            val fallbackBytes = compressJpeg(fallbackScaled, 10)
            if (fallbackScaled !== inputBitmap && !fallbackScaled.isRecycled) {
                fallbackScaled.recycle()
            }

            lastAttemptBytes = fallbackBytes
            lastAttemptQuality = 10
            lastAttemptW = currentW
            lastAttemptH = currentH

            val (nextW, nextH) = calculateNextDownscale(currentW, currentH)
            if (nextW == currentW && nextH == currentH) {
                break
            }
            currentW = nextW
            currentH = nextH
        }

        val finalBytes = lastAttemptBytes ?: compressJpeg(inputBitmap, 10)
        return ResizerResult(
            outputBytes = finalBytes,
            outputWidth = lastAttemptW,
            outputHeight = lastAttemptH,
            finalQuality = lastAttemptQuality,
            finalSizeBytes = finalBytes.size.toLong()
        )
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
        return stream.toByteArray()
    }
}
