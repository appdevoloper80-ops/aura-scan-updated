package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long,
    val pageCount: Int,
    val pdfPath: String,
    val pagePathsJson: String // Comma or newline separated or JSON string of file paths
) {
    fun getPagePaths(): List<String> {
        return if (pagePathsJson.isBlank()) {
            emptyList()
        } else {
            pagePathsJson.split("\n").filter { it.isNotBlank() }
        }
    }
}
