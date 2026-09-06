package com.openvideo.app.di

import android.content.Context
import androidx.room.Room
import com.openvideo.app.core.prefs.AppPrefs
import com.openvideo.app.core.prefs.PlayerPrefs
import com.openvideo.app.data.local.DatabaseMigrations
import com.openvideo.app.data.local.FavoriteDao
import com.openvideo.app.data.local.HistoryDao
import com.openvideo.app.data.local.MediaIdentityDao
import com.openvideo.app.data.local.MediaSourceDao
import com.openvideo.app.data.local.NetworkRecentItemDao
import com.openvideo.app.data.local.PlaylistDao
import com.openvideo.app.data.local.SeriesEpisodeDao
import com.openvideo.app.data.local.VideoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
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
    fun provideSeriesEpisodeDao(db: VideoDatabase): SeriesEpisodeDao = db.seriesEpisodeDao()

    @Provides
    fun provideMediaSourceDao(db: VideoDatabase): MediaSourceDao = db.mediaSourceDao()

    @Provides
    fun provideNetworkRecentItemDao(db: VideoDatabase): NetworkRecentItemDao = db.networkRecentItemDao()

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

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
}
