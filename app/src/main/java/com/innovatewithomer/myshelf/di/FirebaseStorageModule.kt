package com.innovatewithomer.myshelf.di

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseStorageModule {

    @Provides
    @Singleton
    fun firebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    fun providesFirebaseAppCheck(): FirebaseAppCheck {
        return FirebaseAppCheck.getInstance()
    }
}