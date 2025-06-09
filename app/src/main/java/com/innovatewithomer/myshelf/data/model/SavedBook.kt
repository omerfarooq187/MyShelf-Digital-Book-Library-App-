package com.innovatewithomer.myshelf.data.model

data class SavedBook (
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val fileUrl: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)