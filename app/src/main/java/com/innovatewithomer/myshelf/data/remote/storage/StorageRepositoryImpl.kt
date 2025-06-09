package com.innovatewithomer.myshelf.data.remote.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage
): StorageRepository {
    override suspend fun uploadBookFile(userId: String, uri: Uri): Result<String> {
        return try {

            val fileName = UUID.randomUUID().toString() + ".pdf"
            val ref = storage.reference.child("books/$userId/$fileName")
            ref.putFile(uri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
