package com.example

import com.example.engine.ResizerEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResizerEngineTest {

    @Test
    fun testCalculateTargetDimensionsKeepAspectRatio() {
        // Source 1000x2000 (1:2 ratio), Target 200x230
        // Scaled dimensions should fit inside 200x230 preserving 1:2 ratio -> 115x230
        val (w, h) = ResizerEngine.calculateTargetDimensions(
            inputWidth = 1000,
            inputHeight = 2000,
            targetWidth = 200,
            targetHeight = 230,
            keepAspectRatio = true
        )
        assertTrue("Width must be <= 200", w <= 200)
        assertTrue("Height must be <= 230", h <= 230)
        assertEquals(115, w)
        assertEquals(230, h)
    }

    @Test
    fun testCalculateTargetDimensionsExact() {
        val (w, h) = ResizerEngine.calculateTargetDimensions(
            inputWidth = 1000,
            inputHeight = 2000,
            targetWidth = 140,
            targetHeight = 60,
            keepAspectRatio = false
        )
        assertEquals(140, w)
        assertEquals(60, h)
    }

    @Test
    fun testBinarySearchQualityFindsHighestQualityUnderLimit() {
        val maxTargetBytes = 50 * 1024L // 50 KB
        // Simulated monotonic size curve where size increases with quality
        val sizeFunction: (Int) -> Long = { quality ->
            // Quality 10 -> 10 KB, Quality 80 -> 48 KB, Quality 85 -> 51 KB
            (quality * 600L)
        }

        val optimalQuality = ResizerEngine.binarySearchQuality(
            maxFileSizeBytes = maxTargetBytes,
            getSizeForQuality = sizeFunction
        )

        assertNotNull("Should find optimal quality", optimalQuality)
        assertTrue("Optimal quality size must be <= limit", sizeFunction(optimalQuality!!) <= maxTargetBytes)
        // 50 * 1024 = 51200. 51200 / 600 = 85.33 -> 85
        assertEquals(85, optimalQuality)
    }

    @Test
    fun testCalculateNextDownscaleReducesBy5Percent() {
        val (nextW, nextH) = ResizerEngine.calculateNextDownscale(200, 200)
        assertEquals(190, nextW)
        assertEquals(190, nextH)
    }
}
