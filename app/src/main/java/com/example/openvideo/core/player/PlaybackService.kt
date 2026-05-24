package com.example.openvideo.core.player

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.ActivityOptions
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.session.MediaButtonReceiver
import com.example.openvideo.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : Service() {

    @Inject lateinit var playerManager: PlayerManager
    @Inject lateinit var mediaSessionManager: MediaSessionManager
    @Inject lateinit var playbackCoordinator: PlaybackNotificationCoordinator

    private var mediaSession: MediaSessionCompat? = null
    private val refreshHandler = Handler(Looper.getMainLooper())
    private var progressUpdatesRunning = false

    private var lastUiNotifyKey: String? = null
    private var lastProgressBucket: Long = -1L
    private var lastMetadataTitle: String? = null
    private var lastMetadataDuration: Long? = null
    private val notificationRenderer by lazy { PlaybackNotificationRenderer(this) }
    private val foregroundNotification by lazy { PlaybackForegroundNotification(this) }

    companion object {
        const val ACTION_START = "com.example.openvideo.action.START_PLAYBACK_SERVICE"
        const val ACTION_REFRESH = "com.example.openvideo.action.REFRESH_PLAYBACK_NOTIFICATION"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.openvideo.action.TOGGLE_PLAY_PAUSE"
        const val ACTION_SKIP_TO_NEXT = "com.example.openvideo.action.SKIP_TO_NEXT"
        const val ACTION_SKIP_TO_PREVIOUS = "com.example.openvideo.action.SKIP_TO_PREVIOUS"
        const val ACTION_SEEK_TO_MS = "com.example.openvideo.action.SEEK_TO_MS"
        const val ACTION_DISMISS = "com.example.openvideo.action.DISMISS_PLAYBACK_NOTIFICATION"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_SEEK_POSITION_MS = "extra_seek_position_ms"

        // v2：提高重要性；已创建的 Channel 无法通过代码升级
        private const val NOTIFICATION_ID = 1
        private const val PLAYING_REFRESH_MS = 500L
        private const val PAUSED_REFRESH_MS = 2000L

        private const val REQUEST_OPEN_PLAYER = 10
    }

    private val progressRefreshRunnable = object : Runnable {
        override fun run() {
            if (!progressUpdatesRunning) return
            publishNotification()
            val delay = if (playerManager.isPlaying) PLAYING_REFRESH_MS else PAUSED_REFRESH_MS
            refreshHandler.postDelayed(this, delay)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        foregroundNotification.createNotificationChannel()
        // Android 12+ 前台服务须尽快 startForeground，避免自定义视图构建失败时无通知
        startForeground(NOTIFICATION_ID, foregroundNotification.buildPlaceholderNotification())
        setupMediaSession()
    }

    private fun setupMediaSession() {
        mediaSession = PlaybackServiceMediaSessionConnector(
            mediaSessionManager = mediaSessionManager,
            playerManager = playerManager,
            onTogglePlayPause = ::handleTogglePlayPause,
            onSkipToNext = ::handleSkipToNext,
            onSkipToPrevious = ::handleSkipToPrevious,
            onSeekChanged = ::publishNotification,
            onStop = ::dismissNotificationAndStop
        ).create()
    }

    private fun startProgressUpdates() {
        progressUpdatesRunning = true
        refreshHandler.removeCallbacks(progressRefreshRunnable)
        refreshHandler.post(progressRefreshRunnable)
    }

    private fun stopProgressUpdates() {
        progressUpdatesRunning = false
        refreshHandler.removeCallbacks(progressRefreshRunnable)
    }

    private fun handleTogglePlayPause() {
        if (playbackCoordinator.hasActivityHandlers()) {
            playbackCoordinator.onTogglePlayPause?.invoke()
        } else {
            playerManager.togglePlayPause()
        }
        publishNotification()
        if (progressUpdatesRunning) {
            refreshHandler.removeCallbacks(progressRefreshRunnable)
            refreshHandler.post(progressRefreshRunnable)
        }
    }

    private fun handleSkipToNext() {
        if (playbackCoordinator.hasActivityHandlers()) {
            playbackCoordinator.onSkipToNext?.invoke()
        }
        publishNotification()
    }

    private fun handleSkipToPrevious() {
        if (playbackCoordinator.hasActivityHandlers()) {
            playbackCoordinator.onSkipToPrevious?.invoke()
        }
        publishNotification()
    }

    private fun handleSeekToMs(positionMs: Long) {
        playerManager.seekTo(positionMs)
        publishNotification()
        if (progressUpdatesRunning) {
            refreshHandler.removeCallbacks(progressRefreshRunnable)
            refreshHandler.post(progressRefreshRunnable)
        }
    }

    private fun publishNotification(
        titleOverride: String? = null,
        isPlayingOverride: Boolean? = null
    ) {
        val payload = resolveNotificationPayload(titleOverride, isPlayingOverride)
        playbackCoordinator.updatePlaybackProgress(
            payload.isPlaying,
            payload.positionMs,
            payload.durationMs
        )
        syncMediaSession(payload.isPlaying, payload.positionMs)
        if (payload.title != lastMetadataTitle || payload.durationMs != lastMetadataDuration) {
            mediaSessionManager.updateMetadata(payload.title, payload.durationMs)
            lastMetadataTitle = payload.title
            lastMetadataDuration = payload.durationMs
        }

        val uiKey = playbackNotificationUiKey(payload)
        val progressBucket = PlaybackNotificationProgressPolicy.progressBucket(payload.positionMs)
        if (uiKey == lastUiNotifyKey && progressBucket == lastProgressBucket) {
            return
        }
        lastUiNotifyKey = uiKey
        lastProgressBucket = progressBucket

        startForeground(NOTIFICATION_ID, notificationRenderer.build(payload))
    }

    private fun playbackNotificationUiKey(payload: PlaybackNotificationPayload): String {
        return listOf(
            payload.title,
            payload.isPlaying,
            payload.canSkipToPrevious,
            payload.canSkipToNext,
            payload.durationMs
        ).joinToString("|")
    }

    private fun resolveNotificationPayload(
        titleOverride: String?,
        isPlayingOverride: Boolean?
    ): PlaybackNotificationPayload {
        val snapshot = playbackCoordinator.snapshot
        val title = titleOverride?.takeIf { it.isNotBlank() }
            ?: snapshot?.title?.takeIf { it.isNotBlank() }
            ?: getString(R.string.app_name)
        val player = playerManager.player
        val isPlaying = isPlayingOverride
            ?: player?.isPlaying
            ?: snapshot?.isPlaying
            ?: false
        val positionMs = player?.currentPosition ?: snapshot?.positionMs ?: 0L
        val durationMs = player?.duration?.takeIf { it > 0 } ?: snapshot?.durationMs ?: 0L
        val contentIntent = snapshot?.let {
            PendingIntent.getActivity(
                this,
                REQUEST_OPEN_PLAYER,
                PlaybackServiceIntents.openPlayer(this, it),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ActivityOptions.makeCustomAnimation(this, 0, 0).toBundle()
            )
        }
        val skipCapabilities = PlaybackNotificationSkipCapabilityPolicy.fromSnapshot(snapshot)
        return PlaybackNotificationPayload(
            title = title,
            statusText = notificationStatusText(isPlaying),
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            canSkipToPrevious = skipCapabilities.canSkipToPrevious,
            canSkipToNext = skipCapabilities.canSkipToNext,
            contentIntent = contentIntent
        )
    }

    private fun notificationStatusText(isPlaying: Boolean): String =
        getString(
            if (isPlaying) R.string.playback_notification_status_playing
            else R.string.playback_notification_status_paused
        )

    private fun syncMediaSession(isPlaying: Boolean, positionMs: Long) {
        val skipCapabilities = PlaybackNotificationSkipCapabilityPolicy.fromSnapshot(
            playbackCoordinator.snapshot
        )
        val speed = if (isPlaying) {
            playerManager.player?.playbackParameters?.speed ?: 1f
        } else {
            0f
        }
        val update = MediaSessionPlaybackStatePolicy.resolve(
            isPlaying = isPlaying,
            positionMs = positionMs,
            speed = speed,
            canSkipToNext = skipCapabilities.canSkipToNext,
            canSkipToPrevious = skipCapabilities.canSkipToPrevious
        )
        mediaSessionManager.updatePlaybackState(
            update.state,
            update.positionMs,
            update.speed,
            update.actions
        )
    }

    private fun dismissNotificationAndStop() {
        stopProgressUpdates()
        lastUiNotifyKey = null
        lastProgressBucket = -1L
        lastMetadataTitle = null
        lastMetadataDuration = null
        playbackCoordinator.clearSnapshot()
        mediaSessionManager.markStopped()
        mediaSessionManager.clearMetadata()
        playerManager.player?.pause()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService<NotificationManager>()?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                dismissNotificationAndStop()
                return START_NOT_STICKY
            }
        }

        MediaButtonReceiver.handleIntent(mediaSession, intent)
        when (intent?.action) {
            ACTION_START -> {
                lastUiNotifyKey = null
                lastProgressBucket = -1L
                lastMetadataTitle = null
                lastMetadataDuration = null
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, playerManager.isPlaying)
                publishNotification(titleOverride = title, isPlayingOverride = isPlaying)
                startProgressUpdates()
            }
            ACTION_REFRESH -> {
                publishNotification()
                if (!progressUpdatesRunning) startProgressUpdates()
            }
            ACTION_TOGGLE_PLAY_PAUSE -> handleTogglePlayPause()
            ACTION_SKIP_TO_NEXT -> handleSkipToNext()
            ACTION_SKIP_TO_PREVIOUS -> handleSkipToPrevious()
            ACTION_SEEK_TO_MS -> {
                val positionMs = intent.getLongExtra(EXTRA_SEEK_POSITION_MS, 0L)
                handleSeekToMs(positionMs)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopProgressUpdates()
        getSystemService<NotificationManager>()?.cancel(NOTIFICATION_ID)
        super.onDestroy()
        mediaSessionManager.release()
    }
}
