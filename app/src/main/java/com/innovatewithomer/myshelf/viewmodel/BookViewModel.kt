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
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.innovatewithomer.myshelf.data.local.UserPreferences
import com.innovatewithomer.myshelf.data.local.entity.BookEntity
import com.innovatewithomer.myshelf.data.model.SavedBook
import com.innovatewithomer.myshelf.data.remote.firestore.BookRepository
import com.innovatewithomer.myshelf.data.remote.storage.StorageRepository
import com.innovatewithomer.myshelf.data.repo.BookCacheRepository
import com.innovatewithomer.myshelf.worker.BookSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val storageRepository: StorageRepository,
    private val bookCacheRepository: BookCacheRepository,
    private val preferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    private val _books = MutableStateFlow<List<BookEntity>?>(emptyList())
    val books = _books.asStateFlow()

    private var _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var isOfflineMode = false

    fun loadCachedBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            _books.value = bookCacheRepository.getBooks()
            _isLoading.value = false
        }
    }

    init {
        viewModelScope.launch {
            preferences.isOfflineMode.collect { isOffline->
                isOfflineMode = isOffline
                loadCachedBooks()
            }
        }
        scheduleBackgroundSync()
    }

    fun addBook(userId: String, uri: Uri) {
        viewModelScope.launch {
            val fileName = getFileNameFromUri(context, uri) ?: "Untitled"
            val title = fileName.substringBeforeLast(".pdf", fileName)
            val author = "Unknown"
            val bookId = UUID.randomUUID().toString()

            val localFile = File(context.getExternalFilesDir("pdfs"), "$title.pdf")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                _uploadState.value = UploadState.Failure("Failed to save locally")
                e.printStackTrace()
                return@launch
            }

            val unsyncedBook = BookEntity(
                id = bookId,
                title = title,
                author = author,
                fileUrl = localFile.absolutePath,
                isSynced = false
            )

            bookCacheRepository.insertBook(unsyncedBook)
            loadCachedBooks()

            // Only try cloud upload if NOT offline
            if (!isOfflineMode && isInternetAvailable(context)) {
                try {
                    _uploadState.value = UploadState.Uploading
                    Toast.makeText(context, "Uploading file to cloud", Toast.LENGTH_SHORT).show()

                    val storageResult = storageRepository.uploadBookFile(userId, uri)
                    val fileUrl = storageResult.getOrThrow()
                    val book = SavedBook(bookId, title, author, fileUrl)

                    val firestoreResult = bookRepository.saveBook(userId, book)


                    if (firestoreResult.isSuccess) {
                        bookCacheRepository.updateBookSyncStatus(bookId, fileUrl, true)
                        Toast.makeText(context, "Upload Successful", Toast.LENGTH_SHORT).show()
                        _uploadState.value = UploadState.Success(book)
                    }
                    loadCachedBooks()
                } catch (e: Exception) {
                    _uploadState.value = UploadState.Failure(e.message ?: "Upload failed")
                    scheduleBackgroundSync()
                }
            } else {
                Toast.makeText(context, "Saved locally only (offline mode)", Toast.LENGTH_SHORT).show()
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
        if (!isOfflineMode) {
            val result = bookRepository.getSavedBooks(userId)
            if (result.isSuccess) {
                val booksFromFirebase = result.getOrNull().orEmpty()
                bookCacheRepository.cacheBooks(booksFromFirebase)
            } else {
                Log.e("BookViewModel", "Error getting books", result.exceptionOrNull())
            }
        }
        loadCachedBooks()
        _isLoading.value = false
    }


    fun deleteBook(userId: String, bookEntity: BookEntity) {
        viewModelScope.launch {
            Toast.makeText(context, "Deleting Book...", Toast.LENGTH_SHORT).show()
            val bookId = bookEntity.id
            val isSynced = bookEntity.isSynced

            bookCacheRepository.deleteBook(bookEntity)

            val file = File(bookEntity.fileUrl)
            if (file.exists()) file.delete()

            loadCachedBooks()

            if (isSynced && !isOfflineMode) {
                try {
                    val result = bookRepository.deleteBook(userId, bookId)
                    if (result.isFailure) {
                        bookCacheRepository.insertBook(bookEntity)
                    }
                } catch (e: Exception) {
                    bookCacheRepository.insertBook(bookEntity)
                }
            }
        }
    }


    fun reAddBook(book: BookEntity) {
        viewModelScope.launch {
            try {
                val file = File(book.fileUrl)
                val isLocal = file.exists()

                val restoredFile = if (isLocal) {
                    file
                } else if (!isOfflineMode && book.fileUrl.startsWith("http")) {
                    val fileName = "${book.id}.pdf"
                    val localFile = File(context.getExternalFilesDir("pdfs"), fileName)

                    withContext(Dispatchers.IO) {
                        val input = URL(book.fileUrl).openStream()
                        localFile.outputStream().use { it.write(input.readBytes()) }
                    }
                    localFile
                } else {
                    Toast.makeText(context, "Cannot restore file (offline)", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val restoredBook = book.copy(
                    fileUrl = restoredFile.absolutePath,
                    isSynced = false
                )
                bookCacheRepository.insertBook(restoredBook)
                loadCachedBooks()
                scheduleBackgroundSync()
            } catch (e: Exception) {
                Log.e("UndoDownload", "Failed to restore book", e)
                Toast.makeText(context, "Failed to restore book", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun removeLocally(book: BookEntity) {
        viewModelScope.launch {
            bookCacheRepository.deleteBook(book)
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
