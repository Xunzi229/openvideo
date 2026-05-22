package com.example.openvideo.core.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
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
    private var lastMetadataTitle: String? = null
    private var lastMetadataDuration: Long? = null

    companion object {
        const val ACTION_START = "com.example.openvideo.action.START_PLAYBACK_SERVICE"
        const val ACTION_REFRESH = "com.example.openvideo.action.REFRESH_PLAYBACK_NOTIFICATION"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.openvideo.action.TOGGLE_PLAY_PAUSE"
        const val ACTION_SKIP_TO_NEXT = "com.example.openvideo.action.SKIP_TO_NEXT"
        const val ACTION_SKIP_TO_PREVIOUS = "com.example.openvideo.action.SKIP_TO_PREVIOUS"
        const val ACTION_DISMISS = "com.example.openvideo.action.DISMISS_PLAYBACK_NOTIFICATION"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        // v2：提高重要性；已创建的 Channel 无法通过代码升级
        private const val CHANNEL_ID = "playback_channel_v2"
        private const val NOTIFICATION_ID = 1
        private const val PLAYING_REFRESH_MS = 500L
        private const val PAUSED_REFRESH_MS = 2000L

        private const val REQUEST_OPEN_PLAYER = 10
        private const val REQUEST_PREVIOUS = 11
        private const val REQUEST_TOGGLE = 12
        private const val REQUEST_NEXT = 13
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
        createNotificationChannel()
        // Android 12+ 前台服务须尽快 startForeground，避免自定义视图构建失败时无通知
        startForeground(NOTIFICATION_ID, buildPlaceholderNotification())
        setupMediaSession()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.playback_notification_channel_description)
                setShowBadge(false)
            }
            getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }

    private fun buildPlaceholderNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_playback)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.playback_notification_status_playing))
            .setOngoing(true)
            .build()

    private fun setupMediaSession() {
        mediaSession = mediaSessionManager.create(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                handleTogglePlayPause()
            }

            override fun onPause() {
                handleTogglePlayPause()
            }

            override fun onSkipToNext() {
                handleSkipToNext()
            }

            override fun onSkipToPrevious() {
                handleSkipToPrevious()
            }

            override fun onSeekTo(pos: Long) {
                playerManager.seekTo(pos)
                publishNotification()
            }

            override fun onStop() {
                dismissNotificationAndStop()
            }
        })
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
        if (uiKey == lastUiNotifyKey) {
            return
        }
        lastUiNotifyKey = uiKey

        startForeground(NOTIFICATION_ID, buildCustomNotification(payload))
    }

    private fun playbackNotificationUiKey(payload: NotificationPayload): String {
        return listOf(
            payload.title,
            payload.isPlaying,
            payload.canSkipToPrevious,
            payload.canSkipToNext,
            payload.durationMs
        ).joinToString("|")
    }

    private data class NotificationPayload(
        val title: String,
        val statusText: String,
        val isPlaying: Boolean,
        val positionMs: Long,
        val durationMs: Long,
        val canSkipToPrevious: Boolean,
        val canSkipToNext: Boolean,
        val contentIntent: PendingIntent?
    )

    private fun resolveNotificationPayload(
        titleOverride: String?,
        isPlayingOverride: Boolean?
    ): NotificationPayload {
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
        return NotificationPayload(
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

    private fun buildCustomNotification(payload: NotificationPayload): android.app.Notification {
        val remoteViews = buildPlaybackRemoteViews(payload)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_playback)
            .setContentTitle(payload.title)
            .setContentText(payload.statusText)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(payload.isPlaying)
            .setContentIntent(payload.contentIntent)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .build()
    }

    private fun buildPlaybackRemoteViews(payload: NotificationPayload): RemoteViews {
        val views = RemoteViews(packageName, R.layout.notification_playback)
        views.setTextViewText(R.id.notification_title, payload.title)
        views.setTextViewText(R.id.notification_status_text, payload.statusText)
        views.setTextColor(
            R.id.notification_title,
            ContextCompat.getColor(this, R.color.playback_notification_title)
        )
        views.setTextColor(
            R.id.notification_status_text,
            ContextCompat.getColor(this, R.color.playback_notification_status)
        )
        payload.contentIntent?.let { openPlayerIntent ->
            views.setOnClickPendingIntent(R.id.notification_root, openPlayerIntent)
            views.setOnClickPendingIntent(R.id.notification_title, openPlayerIntent)
            views.setOnClickPendingIntent(R.id.notification_status_text, openPlayerIntent)
        }

        views.setImageViewResource(
            R.id.btn_play_pause,
            if (payload.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        views.setOnClickPendingIntent(
            R.id.btn_play_pause,
            servicePendingIntent(REQUEST_TOGGLE, ACTION_TOGGLE_PLAY_PAUSE)
        )

        views.setViewVisibility(
            R.id.btn_previous,
            if (payload.canSkipToPrevious) View.VISIBLE else View.INVISIBLE
        )
        if (payload.canSkipToPrevious) {
            views.setOnClickPendingIntent(
                R.id.btn_previous,
                servicePendingIntent(REQUEST_PREVIOUS, ACTION_SKIP_TO_PREVIOUS)
            )
        }

        views.setViewVisibility(
            R.id.btn_next,
            if (payload.canSkipToNext) View.VISIBLE else View.INVISIBLE
        )
        if (payload.canSkipToNext) {
            views.setOnClickPendingIntent(
                R.id.btn_next,
                servicePendingIntent(REQUEST_NEXT, ACTION_SKIP_TO_NEXT)
            )
        }

        return views
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

    private fun servicePendingIntent(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, PlaybackService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun dismissNotificationAndStop() {
        stopProgressUpdates()
        lastUiNotifyKey = null
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
