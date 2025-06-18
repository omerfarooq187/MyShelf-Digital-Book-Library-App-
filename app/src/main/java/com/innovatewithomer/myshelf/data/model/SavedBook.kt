package com.innovatewithomer.myshelf.data.model

import com.google.firebase.firestore.PropertyName

data class SavedBook (
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val fileUrl: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)