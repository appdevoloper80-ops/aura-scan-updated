package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.DocumentEntity
import java.io.File
import java.io.FileInputStream

object DocumentSharingUtils {

    fun shareDocumentPdf(context: Context, doc: DocumentEntity) {
        try {
            val file = File(doc.pdfPath)
            if (file.exists()) {
                val authority = "${context.packageName}.fileprovider"
                val uri = FileProvider.getUriForFile(context, authority, file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, doc.name)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share PDF"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportPdfToUri(context: Context, sourceFile: File, targetUri: Uri) {
        try {
            if (!sourceFile.exists()) return
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
