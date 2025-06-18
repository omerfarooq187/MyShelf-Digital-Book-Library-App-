package com.innovatewithomer.myshelf.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.innovatewithomer.myshelf.data.model.SavedBook
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BookRepositoryImpl @Inject constructor(private val firestore: FirebaseFirestore): BookRepository {
    override suspend fun saveBook(userId: String, book: SavedBook): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("books")
                .document(book.id)
                .set(book)
                .await()
            Result.success(Unit)
        } catch (e:Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getSavedBooks(userId: String): Result<List<SavedBook>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("books")
                .get()
                .await()

            val books = snapshot.toObjects(SavedBook::class.java)
            Result.success(books)
        } catch (e:Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deleteBook(userId: String, bookId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("books")
                .document(bookId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

}