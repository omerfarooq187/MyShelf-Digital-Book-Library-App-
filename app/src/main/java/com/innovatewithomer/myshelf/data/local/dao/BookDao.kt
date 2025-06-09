package com.innovatewithomer.myshelf.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.innovatewithomer.myshelf.data.local.entity.CachedBook

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<CachedBook>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<CachedBook>)

    @Query("DELETE FROM books")
    suspend fun clearBooks()
}