package com.example.openvideo.di

import android.content.Context
import androidx.room.Room
import com.example.openvideo.data.local.FavoriteDao
import com.example.openvideo.data.local.HistoryDao
import com.example.openvideo.data.local.VideoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VideoDatabase {
        return Room.databaseBuilder(
            context,
            VideoDatabase::class.java,
            "openvideo.db"
        ).build()
    }

    @Provides
    fun provideHistoryDao(db: VideoDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideFavoriteDao(db: VideoDatabase): FavoriteDao = db.favoriteDao()
}
