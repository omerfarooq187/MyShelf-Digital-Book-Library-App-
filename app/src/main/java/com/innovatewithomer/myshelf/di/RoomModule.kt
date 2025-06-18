package com.innovatewithomer.myshelf.di

import android.content.Context
import androidx.room.Room
import com.innovatewithomer.myshelf.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext app:Context) : AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "my_shelf.db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun providesBookDao(db: AppDatabase) = db.bookDao()
}