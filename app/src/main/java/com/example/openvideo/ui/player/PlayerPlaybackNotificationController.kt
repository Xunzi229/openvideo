package com.example.openvideo.ui.player

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.ui.PlayerView
import com.example.openvideo.core.player.PlaybackNotificationCoordinator
import com.example.openvideo.core.player.PlaybackServiceIntents
import com.example.openvideo.core.player.PlaybackQueueSkipPolicy
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.data.model.VideoItem

internal class PlayerPlaybackNotificationController(
    private val activity: AppCompatActivity,
    private val playerPrefs: PlayerPrefs,
    private val viewModel: PlayerViewModel,
    private val playbackCoordinator: PlaybackNotificationCoordinator,
    private val notificationPermissionLauncher: ActivityResultLauncher<String>,
    private val playerViewProvider: () -> PlayerView?,
    private val firstFrameScrimProvider: () -> View?,
    private val titleProvider: () -> String?,
    private val currentVideoUriStringProvider: () -> String,
    private val currentVideoPathProvider: () -> String,
    private val intentProvider: () -> Intent,
    private val isActivityForegroundProvider: () -> Boolean,
    private val isAwaitingFirstFrameProvider: () -> Boolean,
    private val isInPipModeProvider: () -> Boolean,
    private val onTogglePlayPause: () -> Unit,
    private val onSyncPlayPauseIcon: () -> Unit,
    private val onSwitchSessionVideo: (VideoItem, () -> Unit) -> Unit,
    private val onCurrentVideoChanged: (VideoItem) -> Unit,
    private val onLoadSubtitles: (String, String) -> Unit,
    private val onApplyPlayerSettings: () -> Unit,
    private val onScheduleHideControls: () -> Unit
) {
    fun ensurePermission() {
        val requiresPermission =
            PlayerNotificationPermissionPolicy.requiresRuntimePermission(Build.VERSION.SDK_INT)
        if (!requiresPermission) return
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        val permissionPrefs = activity.getSharedPreferences(PREFS_NOTIFICATION_PERMISSION, Context.MODE_PRIVATE)
        val requestedBefore = permissionPrefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
        if (PlayerNotificationPermissionPolicy.shouldRequestPermission(
                requiresPermission = requiresPermission,
                granted = granted,
                requestedBefore = requestedBefore,
                notificationEnabled = playerPrefs.bgPlaybackNotificationEnabled
            )
        ) {
            permissionPrefs.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply()
            runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun registerHandlers() {
        playbackCoordinator.registerHandlers(
            onTogglePlayPause = {
                onTogglePlayPause()
                syncSnapshot()
                refreshIfRunning()
            },
            onSkipToNext = { skipQueueVideo(forward = true) },
            onSkipToPrevious = { skipQueueVideo(forward = false) }
        )
    }

    fun reattachPlayerSurfaceFromBackground() {
        val playerView = playerViewProvider() ?: return
        playerView.visibility = View.VISIBLE
        playerView.player = viewModel.player
        if (!PlayerFirstFrameScrimPolicy.scrimVisibleOnReattach(isAwaitingFirstFrameProvider())) {
            firstFrameScrimProvider()?.visibility = View.GONE
        }
        onSyncPlayPauseIcon()
    }

    fun syncSnapshot() {
        val title = titleProvider() ?: return
        val player = viewModel.player
        val intent = intentProvider()
        playbackCoordinator.updateSnapshot(
            PlaybackNotificationCoordinator.Snapshot(
                videoUri = currentVideoUriStringProvider(),
                title = title,
                videoId = viewModel.playingVideoId,
                videoPath = currentVideoPathProvider(),
                videoWidth = intent.getIntExtra(PlayerActivity.EXTRA_VIDEO_WIDTH, 0),
                videoHeight = intent.getIntExtra(PlayerActivity.EXTRA_VIDEO_HEIGHT, 0),
                queue = viewModel.sessionQueue.value,
                loopMode = playerPrefs.loopMode,
                isPlaying = player?.isPlaying == true,
                positionMs = player?.currentPosition ?: 0L,
                durationMs = player?.duration?.takeIf { it > 0L } ?: 0L
            )
        )
    }

    fun refreshIfRunning() {
        if (!PlayerNotificationRefreshPolicy.shouldRefreshBackgroundPlayback(
                isFinishing = activity.isFinishing,
                backgroundAudio = playerPrefs.bgAudio,
                notificationEnabled = playerPrefs.bgPlaybackNotificationEnabled,
                isActivityForeground = isActivityForegroundProvider()
            )
        ) {
            return
        }
        runCatching {
            ContextCompat.startForegroundService(activity, PlaybackServiceIntents.refresh(activity))
        }
    }

    fun dismiss() {
        playbackCoordinator.clearSnapshot()
        val intent = PlaybackServiceIntents.stop(activity)
        runCatching { activity.startService(intent) }
        runCatching { activity.stopService(intent) }
    }

    fun skipQueueVideo(forward: Boolean) {
        val queue = viewModel.sessionQueue.value
        if (!PlayerSessionQueueChromePolicy.canOpenSessionListPanel(queue.size)) return
        val currentIndex = queue.indexOfFirst { it.id == viewModel.playingVideoId }
        val targetIndex = if (forward) {
            PlayerQueueSkipPolicy.nextIndex(currentIndex, queue.size, playerPrefs.loopMode)
        } else {
            PlayerQueueSkipPolicy.previousIndex(currentIndex, queue.size, playerPrefs.loopMode)
        } ?: return
        val item = queue[targetIndex]
        onSwitchSessionVideo(item) {
            onCurrentVideoChanged(item)
            onLoadSubtitles(
                playerPrefs.externalSubtitleUri.ifBlank { item.uri.toString() },
                item.path
            )
            onApplyPlayerSettings()
            syncSnapshot()
            refreshIfRunning()
            onScheduleHideControls()
        }
    }

    fun startIfNeeded(isPlaying: Boolean) {
        if (activity.isFinishing) return
        val decision = PlayerBackgroundServicePolicy.startDecision(
            backgroundAudio = playerPrefs.bgAudio,
            isPlaying = isPlaying,
            isActivityForeground = isActivityForegroundProvider(),
            isInPictureInPicture = isInPipModeProvider(),
            notificationEnabled = playerPrefs.bgPlaybackNotificationEnabled
        )
        if (!decision.shouldStart) return
        syncSnapshot()
        val intent = PlaybackServiceIntents.start(
            context = activity,
            title = titleProvider().orEmpty(),
            isPlaying = isPlaying
        )
        runCatching { ContextCompat.startForegroundService(activity, intent) }
    }

    fun stop() {
        runCatching { activity.stopService(PlaybackServiceIntents.stop(activity)) }
    }

    private companion object {
        const val PREFS_NOTIFICATION_PERMISSION = "notification_permission"
        const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    }
}
