package com.innovatewithomer.myshelf.utils

import com.innovatewithomer.myshelf.data.local.entity.BookEntity
import com.innovatewithomer.myshelf.data.model.SavedBook

fun BookEntity.toSavedBook(): SavedBook {
    return SavedBook(
        id = id,
        title = title,
        author = author,
        fileUrl = fileUrl
    )
}

fun SavedBook.toBookEntity(isSynced: Boolean = true): BookEntity {
    return BookEntity(
        id = id,
        title = title,
        author = author,
        fileUrl = fileUrl,
        isSynced = isSynced
    )
}
