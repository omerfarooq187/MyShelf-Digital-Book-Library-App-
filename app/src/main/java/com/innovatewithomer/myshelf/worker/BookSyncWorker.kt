package com.innovatewithomer.myshelf.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.appcheck.FirebaseAppCheck
import com.innovatewithomer.myshelf.data.local.entity.BookEntity
import com.innovatewithomer.myshelf.data.model.SavedBook
import com.innovatewithomer.myshelf.data.remote.auth.AuthRepository
import com.innovatewithomer.myshelf.data.remote.firestore.BookRepository
import com.innovatewithomer.myshelf.data.remote.storage.StorageRepository
import com.innovatewithomer.myshelf.data.repo.BookCacheRepository
import com.innovatewithomer.myshelf.utils.toSavedBook
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await
import java.io.File

@HiltWorker
class BookSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val authRepository: AuthRepository,
    private val bookCacheRepository: BookCacheRepository,
    private val bookRepository: BookRepository,
    private val storageRepository: StorageRepository,  // Add this dependency
    private val firebaseCheck: FirebaseAppCheck
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return try {

            val userId = authRepository.currentUser?.uid ?: return Result.failure()
            val unsyncedBooks = bookCacheRepository.getUnsyncedBooks()

            if (unsyncedBooks.isEmpty()) return Result.success()

            // Process books in batches to avoid timeouts
            unsyncedBooks.chunked(5).forEach { batch ->
                processBookBatch(userId, batch)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun processBookBatch(userId: String, books: List<BookEntity>) {
        books.forEach { bookEntity ->
            try {
                // 1. Upload file to Firebase Storage
                val localFile = File(bookEntity.fileUrl)
                if (!localFile.exists()) {
                    Log.e("BookSyncWorker", "Local file missing: ${bookEntity.fileUrl}")
                    return@forEach
                }

                val fileUri = Uri.fromFile(localFile)
                val storageResult = storageRepository.uploadBookFile(userId, fileUri)
                if (storageResult.isFailure) {
                    Log.e("BookSyncWorker", "Upload failed: ${storageResult.exceptionOrNull()?.message}")
                    return@forEach
                }

                // 2. Get cloud URL
                val cloudUrl = storageResult.getOrNull() ?: return@forEach

                // 3. Save metadata to Firestore
                val savedBook = SavedBook(
                    id = bookEntity.id,
                    title = bookEntity.title,
                    author = bookEntity.author,
                    fileUrl = cloudUrl
                )

                val firestoreResult = bookRepository.saveBook(userId, savedBook)
                if (firestoreResult.isFailure) {
                    Log.e("BookSyncWorker", "Firestore save failed")
                    return@forEach
                }

                // 4. Update local cache
                bookCacheRepository.updateBookSyncStatus(
                    bookId = bookEntity.id,
                    fileUrl = cloudUrl,  // Update to cloud URL
                    isSynced = true
                )

                Log.d("BookSyncWorker", "Synced book: ${bookEntity.title}")
            } catch (e: Exception) {
                Log.e("BookSyncWorker", "Error syncing book", e)
            }
        }
    }
}