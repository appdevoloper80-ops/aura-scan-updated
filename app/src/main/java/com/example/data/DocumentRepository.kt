package com.example.data

import com.example.data.db.DocumentDao
import com.example.data.model.DocumentEntity
import com.example.util.FileStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val fileStorageManager: FileStorageManager
) {

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    fun observeDocument(id: String): Flow<DocumentEntity?> = documentDao.observeDocumentById(id)

    suspend fun getDocumentById(id: String): DocumentEntity? = withContext(Dispatchers.IO) {
        documentDao.getDocumentById(id)
    }

    suspend fun insertDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        documentDao.insertDocument(document)
    }

    suspend fun renameDocument(id: String, newName: String) = withContext(Dispatchers.IO) {
        documentDao.renameDocument(id, newName.trim())
    }

    suspend fun deleteDocument(id: String) = withContext(Dispatchers.IO) {
        documentDao.deleteDocumentById(id)
        fileStorageManager.deleteDocumentFiles(id)
    }
}
