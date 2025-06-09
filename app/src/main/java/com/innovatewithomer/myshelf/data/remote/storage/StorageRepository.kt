package com.innovatewithomer.myshelf.data.remote.storage

import android.net.Uri

interface StorageRepository {
    suspend fun uploadBookFile(userId: String, uri: Uri): Result<String>
}
