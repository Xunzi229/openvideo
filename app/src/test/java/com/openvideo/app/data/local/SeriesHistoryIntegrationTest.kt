package com.openvideo.app.data.local

import android.app.Application
import android.net.Uri
import androidx.room.Room
import com.openvideo.app.core.prefs.WebDavCredentialStore
import com.openvideo.app.data.model.VideoItem
import com.openvideo.app.data.repository.VideoRepository
import com.openvideo.app.data.scanner.VideoScanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SeriesHistoryIntegrationTest {
    @Test fun rescanningSeriesPreservesAllEpisodesAndTheirIds() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, VideoDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.seriesEpisodeDao()
            val series = SeriesEntity(title = "Show", normalizedTitleKey = "show", folderPath = "/show", createdAt = 1, updatedAt = 1)
            val seriesId = dao.insertSeries(series)
            val episodeIds = (1..2).map { n ->
                val identity = db.mediaIdentityDao().insertIdentity(identity(n.toLong()))
                assertEquals(seriesId, dao.insertSeries(series.copy(updatedAt = n.toLong())))
                dao.upsertEpisode(EpisodeEntity(seriesId = seriesId, identityId = identity, episodeStart = n,
                    confidence = "HIGH", rule = "test", createdAt = 1, updatedAt = 1))
            }
            dao.insertSeries(series.copy(seriesId = seriesId, title = "Updated", updatedAt = 5))
            val episodes = dao.getEpisodesForSeries(seriesId).first()
            assertEquals(listOf(1, 2), episodes.map { it.episodeStart })
            assertEquals(episodeIds, episodes.map { it.episodeId })
            assertEquals("Updated", dao.getAllSeries().first().single().title)
        } finally { db.close() }
    }

    @Test fun metadataFreePlaybackHistoryKeepsIdentityAcrossMediaStoreIdChange() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(context, VideoDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = VideoRepository(VideoScanner(context), db.historyDao(), db.favoriteDao(), db.playlistDao(),
                db.mediaIdentityDao(), db.mediaSourceDao(), db.seriesEpisodeDao(), db.networkRecentItemDao(), WebDavCredentialStore(context))
            val identityId = db.mediaIdentityDao().insertIdentity(identity(1))
            val video = VideoItem(1, "movie", "content://media/external/video/media/1",
                Uri.parse("content://media/external/video/media/1"), 120000, 0, 0, 0, 0, null)
            repo.saveHistory(video, 42000, 1f, "fit", "off", "", true, -1, -1, false)
            assertEquals(identityId, db.historyDao().getByVideoId(1)!!.mediaIdentityId)
            db.mediaIdentityDao().updateIdentity(identity(2).copy(identityId = identityId))
            assertEquals(42000L, repo.getHistory(2)!!.lastPosition)
            // Same numeric ID but a different source must not steal an identity.
            repo.saveHistory(video.copy(id = 2), 1000, 1f, "fit", "off", "", true, -1, -1, false)
            assertNull(db.historyDao().getByVideoId(2)!!.mediaIdentityId)
        } finally { db.close() }
    }

    private fun identity(id: Long) = MediaIdentityEntity(currentVideoId = id, title = "movie",
        currentPath = "content://media/external/video/media/$id", normalizedPathKey = "/movie$id.mp4",
        normalizedTitleKey = "movie", sizeBytes = 1024, durationMs = 120000, width = 640, height = 480,
        modifiedTime = 1, firstSeen = 1, lastSeen = 1)
}
