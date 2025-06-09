package com.innovatewithomer.myshelf.data.repo

import com.innovatewithomer.myshelf.data.local.dao.BookDao
import com.innovatewithomer.myshelf.data.local.entity.CachedBook
import com.innovatewithomer.myshelf.data.model.SavedBook
import javax.inject.Inject

class BookCacheRepository @Inject constructor(
    private val bookDao: BookDao
) {
    suspend fun getBooks(): List<CachedBook> {
        return bookDao.getAllBooks()
    }

    suspend fun cacheBooks(books: List<SavedBook>) {
        bookDao.clearBooks()
        bookDao.insertBooks(
            books.map {
                CachedBook(
                    id = it.id,
                    title = it.title,
                    author = it.author,
                    fileUrl = it.fileUrl
                )
            }
        )
    }
}