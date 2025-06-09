package com.innovatewithomer.myshelf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class CachedBook(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val fileUrl: String
)
