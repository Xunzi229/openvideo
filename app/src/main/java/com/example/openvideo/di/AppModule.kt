package com.example.openvideo.di

import android.content.Context
import androidx.room.Room
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.data.local.DatabaseMigrations
import com.example.openvideo.data.local.FavoriteDao
import com.example.openvideo.data.local.HistoryDao
import com.example.openvideo.data.local.MediaIdentityDao
import com.example.openvideo.data.local.PlaylistDao
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
        ).addMigrations(*DatabaseMigrations.ALL).build()
    }

    @Provides
    fun provideHistoryDao(db: VideoDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideFavoriteDao(db: VideoDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun providePlaylistDao(db: VideoDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideMediaIdentityDao(db: VideoDatabase): MediaIdentityDao = db.mediaIdentityDao()

    @Provides
    @Singleton
    fun providePlayerPrefs(@ApplicationContext context: Context): PlayerPrefs {
        return PlayerPrefs(context)
    }

    @Provides
    @Singleton
    fun provideAppPrefs(@ApplicationContext context: Context): AppPrefs {
        return AppPrefs(context)
    }
}
