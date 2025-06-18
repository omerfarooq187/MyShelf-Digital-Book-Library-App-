package com.innovatewithomer.myshelf.data.repo

import com.innovatewithomer.myshelf.data.local.dao.BookDao
import com.innovatewithomer.myshelf.data.local.entity.BookEntity
import com.innovatewithomer.myshelf.data.model.SavedBook
import javax.inject.Inject

class BookCacheRepository @Inject constructor(
    private val bookDao: BookDao
) {
    suspend fun getBooks(): List<BookEntity> {
        return bookDao.getAllBooks()
    }

    suspend fun cacheBooks(books: List<SavedBook>) {
        bookDao.insertBooks(
            books.map {
                BookEntity(
                    id = it.id,
                    title = it.title,
                    author = it.author,
                    fileUrl = it.fileUrl,
                    isSynced = true
                )
            }
        )
    }

    suspend fun insertBook(book: BookEntity) {
        bookDao.insertBook(book)
    }

    suspend fun updateBookSyncStatus(bookId: String, fileUrl: String, isSynced: Boolean) {
        bookDao.updateBookSyncStatus(bookId, fileUrl, isSynced)
    }

    suspend fun deleteBook(book: BookEntity) {
        bookDao.deleteBook(book)
    }


    suspend fun getUnsyncedBooks(): List<BookEntity> {
        return bookDao.getUnsyncedBooks()
    }

}