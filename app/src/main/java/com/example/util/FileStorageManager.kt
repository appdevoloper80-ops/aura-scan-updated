package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class FileStorageManager(private val context: Context) {

    private val documentsBaseDir: File
        get() = File(context.filesDir, Constants.DOCUMENTS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }

    fun getDocumentDirectory(documentId: String): File {
        return File(documentsBaseDir, documentId).apply {
            if (!exists()) mkdirs()
        }
    }

    fun copyUriToFile(sourceUri: Uri, destinationFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveBitmapToFile(bitmap: Bitmap, destinationFile: File, quality: Int = 90): Boolean {
        return try {
            FileOutputStream(destinationFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteDocumentFiles(documentId: String): Boolean {
        return try {
            val dir = File(documentsBaseDir, documentId)
            if (dir.exists()) {
                dir.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getShareableUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun createCacheFile(prefix: String, suffix: String): File {
        return File.createTempFile(prefix, suffix, context.cacheDir)
    }
}
