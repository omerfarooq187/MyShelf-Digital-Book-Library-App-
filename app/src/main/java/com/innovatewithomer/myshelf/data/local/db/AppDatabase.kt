package com.innovatewithomer.myshelf.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.innovatewithomer.myshelf.data.local.dao.BookDao
import com.innovatewithomer.myshelf.data.local.entity.BookEntity

@Database(entities = [BookEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun bookDao(): BookDao
}