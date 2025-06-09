package com.innovatewithomer.myshelf.data.remote.firestore

import com.innovatewithomer.myshelf.data.model.SavedBook

interface BookRepository {
    suspend fun saveBook(userId: String, book: SavedBook): Result<Unit>
    suspend fun getSavedBooks(userId: String): Result<List<SavedBook>>
}