package com.innovatewithomer.myshelf.viewmodel

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovatewithomer.myshelf.data.local.entity.CachedBook
import com.innovatewithomer.myshelf.data.model.SavedBook
import com.innovatewithomer.myshelf.data.remote.firestore.BookRepository
import com.innovatewithomer.myshelf.data.remote.storage.StorageRepository
import com.innovatewithomer.myshelf.data.repo.BookCacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val storageRepository: StorageRepository,
    private val bookCacheRepository: BookCacheRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    private val _books = MutableStateFlow<List<CachedBook>?>(emptyList())
    val books = _books.asStateFlow()

    private var _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadCachedBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            _books.value = bookCacheRepository.getBooks()
        }
    }
    fun uploadBook(userId: String, uri: Uri) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading

            // Step 1: Upload file to Firebase Storage
            val storageResult = storageRepository.uploadBookFile(userId, uri)
            if (storageResult.isFailure) {
                _uploadState.value = UploadState.Failure("Upload Failed: ${storageResult.exceptionOrNull()?.message}")
                return@launch
            }

            val fileUrl = storageResult.getOrNull() ?: return@launch
            val fileName = getFileNameFromUri(context,uri)?:"Untitled"
            val title = fileName.substringBeforeLast(".pdf", fileName)
            val author = "Unknown"

            // Step 2: Create SavedBook metadata
            val book = SavedBook(
                id = UUID.randomUUID().toString(),
                title = title,
                author = author,
                fileUrl = fileUrl
            )

            // Step 3: Save to Firestore
            val firestoreResult = bookRepository.saveBook(userId, book)
            if (firestoreResult.isSuccess) {
                _uploadState.value = UploadState.Success(book)
                getBooks(userId)
            } else {
                _uploadState.value = UploadState.Failure("Save Failed: ${firestoreResult.exceptionOrNull()?.message}")
            }
        }
    }

    suspend fun getBooks(userId: String) {
        _isLoading.value = true
        val result = bookRepository.getSavedBooks(userId)
        if (result.isSuccess) {
            val booksFromFirebase = result.getOrNull().orEmpty()
            bookCacheRepository.cacheBooks(booksFromFirebase)
            loadCachedBooks()
        } else {
            Log.e("BookViewModel", "Error getting books", result.exceptionOrNull())
            _books.value = emptyList() // or null based on your logic
        }
        _isLoading.value = false
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        val returnCursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        returnCursor?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    fun clearUploadState() {
        _uploadState.value = UploadState.Idle
    }

    fun clearCache() {
        viewModelScope.launch {
            bookCacheRepository.cacheBooks(emptyList())
            loadCachedBooks()
        }
    }

}

sealed class UploadState {
    data object Idle : UploadState()
    data object Uploading : UploadState()
    data class Success(val book: SavedBook) : UploadState()
    data class Failure(val message: String) : UploadState()
}
