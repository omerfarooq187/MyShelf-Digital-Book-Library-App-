package com.innovatewithomer.myshelf.viewmodel

import android.content.Context
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.innovatewithomer.myshelf.data.local.entity.BookEntity
import com.innovatewithomer.myshelf.data.model.SavedBook
import com.innovatewithomer.myshelf.data.remote.firestore.BookRepository
import com.innovatewithomer.myshelf.data.remote.storage.StorageRepository
import com.innovatewithomer.myshelf.data.repo.BookCacheRepository
import com.innovatewithomer.myshelf.worker.BookSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
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

    private val _books = MutableStateFlow<List<BookEntity>?>(emptyList())
    val books = _books.asStateFlow()

    private var _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadCachedBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            _books.value = bookCacheRepository.getBooks()
            _isLoading.value = false
        }
    }

    init {
        scheduleBackgroundSync()
    }

    fun addBook(userId: String, uri: Uri) {
        viewModelScope.launch {
            val fileName = getFileNameFromUri(context, uri) ?: "Untitled"
            val title = fileName.substringBeforeLast(".pdf", fileName)
            val author = "Unknown"
            val bookId = UUID.randomUUID().toString()

            // Save locally first
            val localFile = File(context.getExternalFilesDir("pdfs"), "$title.pdf")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Failure("Failed to save locally")
                return@launch
            }

            // Create unsynced book
            val unsyncedBook = BookEntity(
                id = bookId,
                title = title,
                author = author,
                fileUrl = localFile.absolutePath,
                isSynced = false
            )

            // Insert and load once
            bookCacheRepository.insertBook(unsyncedBook)
            loadCachedBooks()

            // Start upload if online
            if (isInternetAvailable(context)) {
                try {
                    _uploadState.value = UploadState.Uploading
                    Toast.makeText(context, "Uploading file to cloud", Toast.LENGTH_SHORT).show()

                    // Upload file
                    val storageResult = storageRepository.uploadBookFile(userId, uri)
                    if (storageResult.isFailure) {
                        throw Exception("Upload failed: ${storageResult.exceptionOrNull()?.message}")
                    }

                    val fileUrl = storageResult.getOrNull() ?: throw Exception("No URL returned")
                    val book = SavedBook(bookId, title, author, fileUrl)

                    // Save to Firestore
                    val firestoreResult = bookRepository.saveBook(userId, book)
                    if (firestoreResult.isFailure) {
                        throw Exception("Firestore save failed")
                    }

                    // UPDATE: Use direct SQL update
                    bookCacheRepository.updateBookSyncStatus(
                        bookId = bookId,
                        fileUrl = fileUrl,
                        isSynced = true
                    )

                    _uploadState.value = UploadState.Success(book)
                    loadCachedBooks()

                } catch (e: Exception) {
                    _uploadState.value = UploadState.Failure(e.message ?: "Upload failed")
                    scheduleBackgroundSync()
                }
            } else {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                scheduleBackgroundSync()
            }
        }
    }


    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnectedOrConnecting == true
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

    fun deleteBook(userId: String, bookEntity: BookEntity) {
        viewModelScope.launch {
            // First get a reference to the book ID before deletion
            val bookId = bookEntity.id
            val isSynced = bookEntity.isSynced

            // 1. First delete from local cache
            bookCacheRepository.deleteBook(bookEntity)

            // 2. Delete local file
            val file = File(bookEntity.fileUrl)
            if (file.exists()) {
                file.delete()
            }

            // 3. Delete from Firestore if synced (do this AFTER local deletion)
            if (isSynced) {
                try {
                    // Add a small delay to ensure local deletion completes
//                    delay(3000)

                    val firestoreResult = bookRepository.deleteBook(userId, bookId)
                    if (firestoreResult.isFailure) {
                        Log.e("BookViewModel", "Failed to delete from Firestore", firestoreResult.exceptionOrNull())
                        // Reinsert if Firestore deletion fails
                        bookCacheRepository.insertBook(bookEntity)
                    }
                } catch (e: Exception) {
                    Log.e("BookViewModel", "Exception deleting from Firestore", e)
                    // Reinsert if Firestore deletion fails
                    bookCacheRepository.insertBook(bookEntity)
                }
            }

            // 4. Refresh local book list
            loadCachedBooks()
        }
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

    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<BookSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

sealed class UploadState {
    data object Idle : UploadState()
    data object Uploading : UploadState()
    data class Success(val book: SavedBook) : UploadState()
    data class Failure(val message: String) : UploadState()
}
