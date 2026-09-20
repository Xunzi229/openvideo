package com.openvideo.app.ui.player

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import com.openvideo.app.core.network.WebDavMemoryCache
import com.openvideo.app.core.player.PlayerManager
import com.openvideo.app.core.prefs.PlayerPrefs
import com.openvideo.app.core.prefs.WebDavCredentialStore
import com.openvideo.app.core.subtitle.SubtitleLoader
import com.openvideo.app.data.local.HistoryEntity
import com.openvideo.app.data.local.VideoDatabase
import com.openvideo.app.data.model.VideoItem
import com.openvideo.app.data.repository.VideoRepository
import com.openvideo.app.data.scanner.VideoScanner
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PlayerReviewRegressionTest {
    private lateinit var db: VideoDatabase
    private lateinit var vm: PlayerViewModel
    private lateinit var prefs: PlayerPrefs
    private val store = ViewModelStore()
    private val uri = Uri.parse("https://host/video.mp4")
    private var ready = true
    private var duration = 120_000L
    private var position = 42_000L
    private var state = Player.STATE_READY
    private var mediaLoads = 0
    private var playbackError: PlaybackException? = null
    private lateinit var parameters: TrackSelectionParameters
    private val listeners = mutableListOf<Player.Listener>()
    private val group = TrackGroup(
        Format.Builder().setSampleMimeType("audio/aac").setLanguage("en").build(),
        Format.Builder().setSampleMimeType("audio/aac").setLanguage("zh").build()
    )
    private val tracks = Tracks(listOf(Tracks.Group(group, false,
        intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED), booleanArrayOf(true, false))))

    @Before fun setup() {
        val context = RuntimeEnvironment.getApplication()
        prefs = PlayerPrefs(context)
        parameters = TrackSelectionParameters.Builder(context).build()
        db = Room.inMemoryDatabaseBuilder(context, VideoDatabase::class.java).allowMainThreadQueries().build()
        val repo = VideoRepository(VideoScanner(context), db.historyDao(), db.favoriteDao(), db.playlistDao(),
            db.mediaIdentityDao(), db.mediaSourceDao(), db.seriesEpisodeDao(), db.networkRecentItemDao(), WebDavCredentialStore(context))
        val player = Proxy.newProxyInstance(ExoPlayer::class.java.classLoader, arrayOf(ExoPlayer::class.java)) { _, method, args ->
            when (method.name) {
                "getCurrentMediaItem" -> MediaItem.fromUri(uri)
                "getPlayWhenReady", "isPlaying" -> ready
                "getDuration" -> duration
                "getCurrentPosition" -> position
                "getPlaybackState" -> state
                "getPlayerError" -> playbackError
                "getCurrentTracks" -> tracks
                "getPlaybackParameters" -> PlaybackParameters.DEFAULT
                "getTrackSelectionParameters" -> parameters
                "setTrackSelectionParameters" -> { parameters = args!![0] as TrackSelectionParameters; null }
                "addListener" -> { listeners.add(args!![0] as Player.Listener); null }
                "removeListener" -> { listeners.remove(args!![0]); null }
                "pause" -> { ready = false; listeners.toList().forEach { it.onPlayWhenReadyChanged(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }; null }
                "setMediaItem" -> { mediaLoads++; null }
                "setPlayWhenReady" -> { ready = args!![0] as Boolean; null }
                else -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Long.TYPE -> 0L
                    java.lang.Float.TYPE -> 0f
                    else -> null
                }
            }
        } as ExoPlayer
        val manager = PlayerManager(context)
        PlayerManager::class.java.getDeclaredField("player").apply { isAccessible = true }.set(manager, player)
        vm = PlayerViewModel(manager, repo, prefs, SubtitleLoader(context, prefs, OkHttpClient(), WebDavMemoryCache()))
        store.put("player", vm)
        assertTrue(vm.adoptActiveSession(uri, "video", 1, uri.toString()))
    }

    @After fun cleanup() { store.clear(); db.close() }

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("Async operation did not finish", condition())
    }

    private fun error() = PlaybackException("timeout", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)

    @Test fun lifecyclePauseCancelsPendingNetworkRetry() {
        assertTrue(vm.handleNetworkAutoRetry(error()))
        prefs.bgAudio = false
        PlayerLifecycleController(vm, prefs, { false }, { false }, {}, {}, {}, {}, {}, {}, {}, {}, {})
            .onPause()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8))
        assertFalse(ready)
        assertEquals(0, mediaLoads)
        assertFalse(vm.handleNetworkAutoRetry(error()))
    }

    @Test fun retryStillRunsWhilePlaybackIsRequested() {
        assertTrue(vm.handleNetworkAutoRetry(error()))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        assertEquals(1, mediaLoads)
        assertTrue(ready)
    }

    @Test fun userCanResumeAfterCancellingRetry() {
        playbackError = androidx.media3.exoplayer.ExoPlaybackException.createForSource(
            java.io.IOException("timeout"), PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        )
        assertTrue(vm.handleNetworkAutoRetry(playbackError!!))
        vm.pausePlayback()
        vm.togglePlayPause()
        assertEquals(1, mediaLoads)
        assertTrue(ready)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(8))
        assertEquals(1, mediaLoads)
    }

    @Test fun historyUsesPlayerDurationForExternalOpenQueue() {
        vm.setSessionQueue(listOf(VideoItem(1, "video", uri.toString(), uri, 0, 0, 0, 0, 0, null)), null)
        vm.saveHistory()
        awaitCondition { runBlocking { db.historyDao().getByVideoId(1)?.duration == 120_000L } }
        duration = C.TIME_UNSET
        position = 43_000L
        vm.setSessionQueue(listOf(VideoItem(1, "video", uri.toString(), uri, 120_000, 0, 0, 0, 0, null)), null)
        vm.saveHistory()
        awaitCondition { runBlocking { db.historyDao().getByVideoId(1)?.lastPosition == 43_000L } }
        assertEquals(120_000L, runBlocking { db.historyDao().getByVideoId(1)!!.duration })
    }

    private fun restoreAudio() {
        runBlocking { db.historyDao().upsert(HistoryEntity(1, title = "video", path = uri.toString(),
            duration = 120_000, lastPosition = 42_000, timestamp = 1, audioTrackGroupIndex = 0, audioTrackIndex = 1)) }
        var restored = false
        vm.restorePlaybackPreferences(1) { restored = true }
        awaitCondition { restored }
    }

    @Test fun audioHistoryRestoresWhenPlayerWasAlreadyReady() {
        restoreAudio()
        assertEquals(listOf(1), parameters.overrides[group]!!.trackIndices)
    }

    @Test fun audioHistoryWaitsForPlayerReadiness() {
        state = Player.STATE_BUFFERING
        restoreAudio()
        assertTrue(parameters.overrides.isEmpty())
        state = Player.STATE_READY
        listeners.toList().forEach { it.onPlaybackStateChanged(state) }
        assertEquals(listOf(1), parameters.overrides[group]!!.trackIndices)
    }

    @Test fun encodingReloadUpdatesBothSubtitlesAndPreservesDisabledSecondary() {
        val subtitleUri = Uri.parse("content://review/captions.srt")
        shadowOf(RuntimeEnvironment.getApplication().contentResolver).registerInputStreamSupplier(subtitleUri) {
            "1\n00:00:01,000 --> 00:00:02,000\n中文\n".byteInputStream(charset("GBK"))
        }
        prefs.subtitleEncoding = "UTF-8"
        prefs.externalSubtitleUri = subtitleUri.toString()
        var primary: PlayerSubtitleLoadApplyDecision? = null
        var secondary: PlayerSubtitleLoadApplyDecision? = null
        vm.loadSubtitles(prefs.externalSubtitleUri, uri.toString(), onFinished = { primary = it })
        vm.loadSecondarySubtitles(prefs.externalSubtitleUri, uri.toString()) { secondary = it }
        awaitCondition { primary != null && secondary != null }
        assertTrue("primary=$primary secondary=$secondary", primary!!.shouldApplyToPlayer && secondary!!.shouldApplyToPlayer)
        assertNotEquals("中文", vm.uiState.value.subtitles.single().text)
        vm.setSecondarySubtitlesEnabled(false)
        prefs.subtitleEncoding = "GBK"
        vm.reloadSubtitlesForEncoding()
        awaitCondition { vm.uiState.value.subtitles.single().text == "中文" &&
            vm.uiState.value.dualSubtitles.secondary.items.single().text == "中文" }
        assertFalse(vm.uiState.value.dualSubtitles.secondary.enabled)
    }
}
