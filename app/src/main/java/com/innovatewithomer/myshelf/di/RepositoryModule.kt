package com.innovatewithomer.myshelf.di

import com.innovatewithomer.myshelf.data.remote.auth.AuthRepository
import com.innovatewithomer.myshelf.data.remote.auth.AuthRepositoryImpl
import com.innovatewithomer.myshelf.data.remote.firestore.BookRepository
import com.innovatewithomer.myshelf.data.remote.firestore.BookRepositoryImpl
import com.innovatewithomer.myshelf.data.remote.storage.StorageRepository
import com.innovatewithomer.myshelf.data.remote.storage.StorageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    fun bindBookRepository(bookRepositoryImpl: BookRepositoryImpl): BookRepository

    @Binds
    @Singleton
    fun bindStorageRepository(storageRepositoryImpl: StorageRepositoryImpl): StorageRepository

}