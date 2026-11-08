package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

enum class PdfPageSize {
    A4,
    FIT_TO_IMAGE
}

data class PdfCreationConfig(
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val addMargin: Boolean = true
)

object PdfEngine {

    private const val A4_WIDTH_PTS = 595
    private const val A4_HEIGHT_PTS = 842
    private const val MARGIN_PTS = 36 // 0.5 inch in points

    suspend fun createPdfFromImages(
        context: Context,
        imageUrisOrPaths: List<String>,
        outputPdfFile: File,
        config: PdfCreationConfig,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        try {
            val total = imageUrisOrPaths.size
            for (i in 0 until total) {
                onProgress(i + 1, total)
                val item = imageUrisOrPaths[i]
                val bitmap: Bitmap? = if (item.startsWith("content://") || item.startsWith("file://")) {
                    OcrEngine.decodeSampledBitmapFromUri(context, Uri.parse(item), 2400, 2400)
                } else {
                    OcrEngine.decodeSampledBitmap(item, 2400, 2400)
                }

                if (bitmap == null) continue

                val margin = if (config.addMargin) MARGIN_PTS else 0

                val (pageWidth, pageHeight, destRect) = when (config.pageSize) {
                    PdfPageSize.A4 -> {
                        val availW = A4_WIDTH_PTS - (margin * 2)
                        val availH = A4_HEIGHT_PTS - (margin * 2)
                        val scale = min(availW.toFloat() / bitmap.width, availH.toFloat() / bitmap.height)
                        val drawW = (bitmap.width * scale).roundToInt()
                        val drawH = (bitmap.height * scale).roundToInt()
                        val left = margin + (availW - drawW) / 2
                        val top = margin + (availH - drawH) / 2
                        Triple(A4_WIDTH_PTS, A4_HEIGHT_PTS, Rect(left, top, left + drawW, top + drawH))
                    }
                    PdfPageSize.FIT_TO_IMAGE -> {
                        val pw = bitmap.width + (margin * 2)
                        val ph = bitmap.height + (margin * 2)
                        Triple(pw, ph, Rect(margin, margin, margin + bitmap.width, margin + bitmap.height))
                    }
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Fill with white background
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDocument.finishPage(page)

                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }

            outputPdfFile.parentFile?.mkdirs()
            FileOutputStream(outputPdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            pdfDocument.close()
        }
    }
}
