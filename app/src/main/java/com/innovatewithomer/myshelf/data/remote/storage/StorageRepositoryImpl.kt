package com.innovatewithomer.myshelf.data.remote.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage
) : StorageRepository {
    override suspend fun uploadBookFile(userId: String, uri: Uri): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.pdf"
            val ref = storage.reference.child("users/$userId/books/$fileName")

            // Upload with metadata
            val metadata = StorageMetadata.Builder()
                .setContentType("application/pdf")
                .setCustomMetadata("owner", userId)
                .build()

            // 1. Upload file
            val uploadTask = ref.putFile(uri, metadata)
            val taskSnapshot = uploadTask.await()

            // 2. Get download URL using metadata reference
            val downloadUrl = taskSnapshot.metadata?.reference?.downloadUrl?.await()?.toString()
                ?: throw Exception("Download URL is null")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}