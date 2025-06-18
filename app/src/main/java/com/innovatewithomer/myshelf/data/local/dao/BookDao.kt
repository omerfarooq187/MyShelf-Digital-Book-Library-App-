package com.innovatewithomer.myshelf.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.innovatewithomer.myshelf.data.local.entity.BookEntity

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE isSynced = 0")
    suspend fun getUnsyncedBooks(): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Transaction
    @Query("UPDATE books SET fileUrl = :fileUrl, isSynced = :isSynced WHERE id = :bookId")
    suspend fun updateBookSyncStatus(bookId: String, fileUrl: String, isSynced: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(book: List<BookEntity>)

    @Delete
    suspend fun deleteBook(bookEntity: BookEntity)

    @Query("DELETE FROM books")
    suspend fun clearBooks()
}