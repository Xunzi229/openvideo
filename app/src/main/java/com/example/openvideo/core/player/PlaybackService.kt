package com.example.openvideo.core.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import com.example.openvideo.R
import com.example.openvideo.ui.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : Service() {

    @Inject lateinit var playerManager: PlayerManager
    @Inject lateinit var mediaSessionManager: MediaSessionManager

    private var mediaSession: MediaSessionCompat? = null

    companion object {
        const val ACTION_START = "com.example.openvideo.action.START_PLAYBACK_SERVICE"
        const val ACTION_STOP = "com.example.openvideo.action.STOP_PLAYBACK_SERVICE"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        private const val CHANNEL_ID = "playback_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "播放控制",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "视频播放控制通知"
            }
            getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = mediaSessionManager.create(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                playerManager.togglePlayPause()
            }

            override fun onPause() {
                playerManager.togglePlayPause()
            }

            override fun onSeekTo(pos: Long) {
                playerManager.seekTo(pos)
            }

            override fun onStop() {
                playerManager.release()
                stopSelf()
            }
        })
    }

    fun updateNotification(title: String, isPlaying: Boolean) {
        val sessionToken = mediaSessionManager.getSessionToken() ?: return
        val intent = Intent(this, PlayerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "暂停",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play,
                "播放",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "正在播放" else "已暂停")
            .setSmallIcon(R.drawable.ic_movie)
            .setContentIntent(pendingIntent)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .addAction(playPauseAction)
            .setOngoing(isPlaying)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                    .ifBlank { getString(R.string.app_name) }
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, playerManager.isPlaying)
                updateNotification(title, isPlaying)
            }
            ACTION_STOP -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionManager.release()
    }
}
