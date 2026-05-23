package com.example.openvideo.ui.player

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as Media3UiR
import com.example.openvideo.R
import com.example.openvideo.core.diagnostics.CrashLogger
import com.example.openvideo.core.media.LocalMediaUriPolicy
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.player.PlaybackNotificationCoordinator
import com.example.openvideo.core.player.PlaybackServiceIntents
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.DoubleTapAction
import com.example.openvideo.core.prefs.GestureAction
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SubtitleBgStyle
import com.example.openvideo.core.subtitle.SubtitleLoader
import com.example.openvideo.data.model.VideoItem
import com.example.openvideo.ui.settings.DefaultPlayerSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    @Inject lateinit var playerManager: PlayerManager
    @Inject lateinit var playerPrefs: PlayerPrefs
    @Inject lateinit var subtitleLoader: SubtitleLoader
    @Inject lateinit var playbackCoordinator: PlaybackNotificationCoordinator
    private val viewModel: PlayerViewModel by viewModels()

    private lateinit var playerView: PlayerView
    private lateinit var playerRoot: View
    private lateinit var firstFrameScrim: View
    private lateinit var controlsContainer: View
    private lateinit var topBar: View
    private lateinit var topScrim: View
    private lateinit var bottomScrim: View
    private lateinit var bottomPanel: View
    private lateinit var toolRow: View
    private lateinit var btnPlay: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private var btnScreenshot: ImageButton? = null
    private var btnAbLoop: ImageButton? = null
    private var btnPip: ImageButton? = null
    private lateinit var btnLock: ImageButton
    private lateinit var btnFullscreen: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvTitle: TextView
    private lateinit var gestureOverlay: View
    private lateinit var seekIndicator: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var brightnessIndicator: View
    private lateinit var brightnessProgress: ProgressBar
    private lateinit var volumeIndicator: View
    private lateinit var volumeProgress: ProgressBar

    /** 横屏布局专有：底部工具栏「列表」入口。 */
    private var btnVideoList: View? = null

    /** 横屏右侧浮层（倍速 / 比例 / 画中画）。 */
    private var landRightFloatColumn: View? = null

    private var contentFrameTransformRetryPosted = false
    private var manualVideoZoom = PlayerVideoZoomState.IDENTITY
    private var cachedBaseContentFrameTransform = PlayerContentFrameTransform.IDENTITY

    private val landscapeGeometryListener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (PlayerConfigurationOrientationPolicy.isLandscape(resources.configuration.orientation)) {
                applyLandscapePlayerGeometry()
            }
        }

    private val handler = Handler(Looper.getMainLooper())
    private var controlsVisible = true
    private var isActivityForeground = false
    private var isSettingsOverlayVisible = false
    private var activePlayerDialog: Dialog? = null
    private var controlsVisibleBeforeSettingsOverlay = true
    private val hideControlsRunnable = Runnable { hideControls() }
    private val hideGestureHudRunnable = Runnable { hideGestureHud() }
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isSeeking) {
                viewModel.updatePosition()
                val state = viewModel.uiState.value
                val seekBarState = PlayerTimeline.seekBarState(state.currentPosition, state.duration)
                seekBar.isEnabled = seekBarState.enabled
                seekBar.max = seekBarState.max
                seekBar.progress = seekBarState.progress
                tvCurrentTime.text = formatTime(state.currentPosition)
                tvTotalTime.text = formatTime(state.duration)
                findViewById<TextView>(R.id.tv_land_speed)?.text =
                    landSpeedLabel(playerPrefs.speed)
                saveProgressPeriodically(state.currentPosition)

                applyPlaybackTickSeek(state.currentPosition, state.duration)
                applySubtitlePresentation()
            }
            handler.postDelayed(this, PlayerPlaybackTickPolicy.UI_TICK_INTERVAL_MS)
        }
    }

    private lateinit var gestureDetector: GestureDetector
    private lateinit var pinchZoomDetector: ScaleGestureDetector
    private var currentBrightness = 0.5f
    private var currentVolume = 0.5f
    private var isSeeking = false

    // AB Loop state
    private var abLoopState = PlayerAbLoopState.IDLE
    private var abLoopPointA: Long = -1
    private var abLoopPointB: Long = -1

    // Screen lock state
    private var isScreenLocked = false
    private var pendingSeekTarget: Long? = null
    private var seekGestureAnchorPositionMs: Long? = null
    private var playerListener: Player.Listener? = null
    private var startupAnalyticsListener: androidx.media3.exoplayer.analytics.AnalyticsListener? = null

    // 一旦用户手动切过屏幕方向（点击全屏按钮等），本次视频会话内禁止自动方向覆盖，
    // 防止 onVideoSizeChanged 等回调把用户操作冲掉。切到下一首视频时复位。
    private var userOverrodeOrientation = false
    private var isLongPressing = false
    private var playbackWasBuffering = false
    private var doubleTapSeekState: DoubleTapSeekState? = null
    private var doubleTapSeekAnchorPositionMs: Long? = null
    private var keepGestureHudAfterActionUp = false
    private var hasSkippedIntro = false
    private var hasSkippedOutro = false
    private val startupTrace = PlayerStartupTrace()
    private var hasLoggedFirstFrame = false
    private var hasLoggedFirstFrameTimeout = false
    private var isFirstFrameTimeoutPosted = false
    private var isAwaitingFirstFrame = true
    private var lastHistorySavedPositionMs = 0L

    /** 单次手势起始亮度/音量（0–1），避免 MOVE 期间重复累加误差 */
    private var brightnessGestureAnchor = 0.5f
    private var volumeGestureAnchor = 0.5f

    private lateinit var pickSubtitleLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    // SharedPreferences listener for external subtitle URI written by settings sheet
    private lateinit var settingsPrefs: SharedPreferences
    private lateinit var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener
    private var currentVideoUriString: String = ""
    private var currentVideoPath: String = ""
    private var exitState = PlayerExitState()
    private var isSwitchingQueueAfterEnded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Do NOT call `delegate.localNightMode = MODE_NIGHT_YES` here:
        // AppCompat's setLocalNightMode mutates the process-wide Resources configuration
        // (uiMode = NIGHT_YES). After leaving the player the cached drawables (e.g. the
        // gradient `bg_app_root` used by Home/Local/Playlist fragments) keep the dark
        // colors that were resolved while the player was running, while the host activity's
        // own light theme stays unchanged - producing the "light status/nav bar + dark
        // fragment in the middle" symptom users see when they back out of the player.
        // The player's own theme (Theme.OpenVideo.Player) already hard-codes every color
        // (player_bg / player_panel_bg / player_title_primary ...) to dark, so it stays
        // dark without forcing the night mode flag globally.
        super.onCreate(savedInstanceState)
        suppressNotificationOpenTransition(intent)
        applyInitialVideoOrientation()
        pickSubtitleLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            val subtitles = subtitleLoader.loadFromUri(uri)
            if (subtitles.isNotEmpty()) {
                viewModel.setSubtitles(subtitles)
                Toast.makeText(this, R.string.player_subtitle_loaded, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.player_subtitle_load_failed, Toast.LENGTH_SHORT).show()
            }
        }
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* 用户拒绝也无需处理：通知不显示，但播放仍可继续 */ }
        ensureNotificationPermission()
        enterImmersiveMode()
        setContentView(R.layout.activity_player)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val decision = PlayerLockGesturePolicy.onBackPressed(isScreenLocked)
                if (decision.revealLockedControls) showLockedControls()
                if (decision.finishPlayer) finishPlayer()
            }
        })

        initViews()
        initGestures()
        initBrightnessAndVolume()

        val uriString = intent.getStringExtra("video_uri")
            ?: playbackCoordinator.snapshot?.videoUri
            ?: run { finish(); return }
        val title = intent.getStringExtra("video_title")
            ?: playbackCoordinator.snapshot?.title
            ?: ""
        val id = intent.getLongExtra("video_id", playbackCoordinator.snapshot?.videoId ?: 0L)
        val videoPath = intent.getStringExtra("video_path")
            ?: playbackCoordinator.snapshot?.videoPath
            ?: ""

        startupTrace.record(PlayerStartupTrace.Events.ACTIVITY_CREATED)
        val sessionQueue = intent.sessionVideoQueue().ifEmpty {
            playbackCoordinator.snapshot?.queue.orEmpty()
        }
        viewModel.setSessionQueue(sessionQueue)

        val warmResume = viewModel.isActiveSessionFor(id)
        if (!warmResume) {
            viewModel.initialize(LocalMediaUriPolicy.playbackUri(uriString), title, id, videoPath)
            startupTrace.record(PlayerStartupTrace.Events.PLAYER_INITIALIZED)
        }

        playerView.player = viewModel.player
        playerView.visibility = View.VISIBLE
        firstFrameScrim.visibility =
            if (PlayerFirstFrameScrimPolicy.initialScrimVisible(isAwaitingFirstFrame, warmResume)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        setupControls()
        refreshSessionListButtonVisibility()
        startupTrace.record(PlayerStartupTrace.Events.PLAYER_VIEW_ATTACHED)
        tvTitle.text = title
        updateLandResolutionBadge()

        // remember current video info for external subtitle callback
        currentVideoUriString = uriString
        currentVideoPath = videoPath
        val explicitStartPositionMs = intent.getLongExtra(EXTRA_START_POSITION_MS, 0L)
        if (warmResume) {
            applyPlayerSettings()
            syncPlayPauseIcon()
            syncPlaybackNotificationSnapshot()
        } else {
            viewModel.restorePlaybackPreferences(id) {
                applyPlayerSettings()
                loadSubtitlesAsync(playerPrefs.externalSubtitleUri.ifBlank { uriString }, videoPath)
            }
        }

        // register prefs listener to auto-load external subtitle when set by the settings sheet
        settingsPrefs = getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == PlayerPrefs.KEY_EXTERNAL_SUBTITLE) {
                val uri = prefs.getString(key, "") ?: ""
                if (uri.isNotBlank()) {
                    loadSubtitlesAsync(uri, currentVideoPath, showToast = true)
                }
            }
            if (key == PlayerPrefs.KEY_BRIGHTNESS_ADJUSTMENT) {
                applyScreenBrightness(playerPrefs.brightnessAdjustment)
                scheduleHideControls()
            }
            if (PlayerPrefs.requiresImmediatePlayerApply(key)) {
                applyPlayerSettings()
                scheduleHideControls()
            }
        }
        settingsPrefs.registerOnSharedPreferenceChangeListener(prefsListener)

        scheduleHideControls()

        // Restore playback position if remember_progress is on
        if (PlayerSessionResumePolicy.shouldRestorePlaybackPosition(playerPrefs.rememberProgress)) {
            viewModel.restorePosition(id, explicitStartPositionMs)
        }

        controlsContainer.post { applyLandscapePlayerGeometry() }
        registerPlaybackNotificationHandlers()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        suppressNotificationOpenTransition(intent)
        reattachPlayerSurfaceFromBackground()
    }

    private fun suppressNotificationOpenTransition(intent: Intent) {
        if (!intent.getBooleanExtra(EXTRA_FROM_PLAYBACK_NOTIFICATION, false)) return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                overrideOpenTransitionCompat()
            }
            else -> {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }
    }

    private fun applyPlayerSettings() {
        viewModel.setAspectRatio(playerPrefs.aspectRatio)
        viewModel.setDecodeMode(PlayerDecodeModePolicy.decodeMode(playerPrefs.softwareAudioDecoder))
        viewModel.setSpeed(
            playerPrefs.speed,
            PlayerPlaybackSettings.pitchFor(playerPrefs.speed, playerPrefs.speedPreservePitch)
        )
        viewModel.setRepeatMode(PlayerPlaybackSettings.repeatModeFor(playerPrefs.loopMode))
        viewModel.setVolumeBoost(playerPrefs.volumeBoost)
        playerManager.setMuted(playerPrefs.audioMuted)
        val colorAdjustments = PlayerVideoColorAdjustmentPolicy.fromPercent(
            contrastPercent = playerPrefs.contrastAdjustment,
            saturationPercent = playerPrefs.saturationAdjustment
        )
        playerManager.applyVideoAdjustments(
            0f,
            colorAdjustments.contrast,
            colorAdjustments.saturation
        )
        applyScreenBrightness(playerPrefs.brightnessAdjustment)
        playerView.alpha = PlayerDisplayVisibilityPolicy.videoLayerAlpha(playerPrefs.videoDisplayEnabled)
        bottomPanel.alpha = 1f
        if (controlsVisible) {
            controlsContainer.animate().cancel()
            controlsContainer.alpha = controlsChromeMaxAlpha()
        }

        if (PlayerScreenOnPolicy.shouldKeepScreenOn(playerPrefs.keepScreenOn)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        applyDisplaySettings()

        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, playerPrefs.subtitleSize.toFloat())
        tvSubtitle.setTextColor(playerPrefs.subtitleColor)
        tvSubtitle.setBackgroundColor(PlayerSubtitleStylePolicy.backgroundColor(playerPrefs.subtitleBgStyle))
        tvSubtitle.post {
            tvSubtitle.translationY = PlayerDisplayAdjustment.subtitleTranslationY(
                playerViewHeightPx = playerView.height,
                position = playerPrefs.subtitlePosition
            )
        }
    }

    internal fun refreshPlayerDisplayFromSettings() {
        applyDisplaySettings()
    }

    private fun applyDisplaySettings() {
        if (!PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio)) {
            manualVideoZoom = PlayerVideoZoomState.IDENTITY
        }
        setPlayerResizeMode()
        applyPlayerContentAspectRatio()
        applyPlayerContentFrameTransform()
        playerView.rotation = playerPrefs.rotation.toFloat()
        playerView.scaleX = PlayerDisplayAdjustment.mirrorScaleX(playerPrefs.mirror)
    }

    private fun initBrightnessAndVolume() {
        val windowBrightness = window.attributes.screenBrightness
        val systemBrightness = try {
            android.provider.Settings.System.getInt(
                contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            ) / 255f
        } catch (_: Exception) {
            null
        }
        currentBrightness = PlayerWindowBrightnessPolicy.initialBrightness(
            windowBrightness = windowBrightness,
            systemBrightnessNormalized = systemBrightness
        )

        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        currentVolume = PlayerWindowBrightnessPolicy.initialVolumeLevel(currentVol, maxVolume)

        brightnessProgress.progress = PlayerWindowBrightnessPolicy.levelToProgressPercent(currentBrightness)
        volumeProgress.progress = PlayerWindowBrightnessPolicy.levelToProgressPercent(currentVolume)
    }

    private fun loadSubtitlesAsync(uriString: String, videoPath: String, showToast: Boolean = false) {
        viewModel.loadSubtitles(uriString, videoPath, showToast) { decision ->
            PlayerSubtitleLoadToastPolicy.messageRes(decision.toastKind)?.let { messageRes ->
                Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
            }
            startupTrace.record(PlayerStartupTrace.Events.SUBTITLE_SCAN_FINISHED)
        }
    }

    private fun initViews() {
        playerRoot = findViewById(R.id.player_root)
        playerView = findViewById(R.id.player_view)
        playerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (this::playerView.isInitialized) {
                applyPlayerContentFrameTransform()
            }
        }
        firstFrameScrim = findViewById(R.id.player_first_frame_scrim)
        controlsContainer = findViewById(R.id.controls_container)
        topBar = findViewById(R.id.top_bar)
        topScrim = findViewById(R.id.top_scrim)
        bottomScrim = findViewById(R.id.bottom_scrim)
        bottomPanel = findViewById(R.id.bottom_panel)
        toolRow = findViewById(R.id.tool_row)
        btnPlay = findViewById(R.id.btn_play)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        btnScreenshot = findViewById(R.id.btn_screenshot)
        btnAbLoop = findViewById(R.id.btn_ab_loop)
        btnPip = findViewById(R.id.btn_pip)
        btnLock = findViewById(R.id.btn_lock)
        btnFullscreen = findViewById(R.id.btn_fullscreen)
        btnBack = findViewById(R.id.btn_back)
        seekBar = findViewById(R.id.seek_bar)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvTotalTime = findViewById(R.id.tv_total_time)
        tvTitle = findViewById(R.id.tv_title)
        gestureOverlay = findViewById(R.id.gesture_overlay)
        seekIndicator = findViewById(R.id.seek_indicator)
        tvSubtitle = findViewById(R.id.tv_subtitle)
        brightnessIndicator = findViewById(R.id.brightness_indicator)
        brightnessProgress = findViewById(R.id.brightness_progress)
        volumeIndicator = findViewById(R.id.volume_indicator)
        volumeProgress = findViewById(R.id.volume_progress)
        btnVideoList = findViewById(R.id.btn_video_list)
        landRightFloatColumn = findViewById(R.id.land_right_float_column)
        controlsContainer.addOnLayoutChangeListener(landscapeGeometryListener)
    }

    private fun updateLandResolutionBadge() {
        val w = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0)
        findViewById<TextView>(R.id.tv_land_resolution_badge)?.visibility =
            if (PlayerLandscapeBadgePolicy.shouldShowBadge(w)) View.VISIBLE else View.GONE
    }

    private fun landSpeedLabel(speed: Float): String = PlayerSpeedLabel.format(speed)

    private fun refreshSessionListButtonVisibility() {
        val show = PlayerSessionQueueChromePolicy.shouldShowSessionListButton(
            viewModel.sessionQueue.value.size
        )
        btnVideoList?.isVisible = show
        findViewById<View>(R.id.portrait_btn_episodes)?.isVisible = show
    }

    private fun showSessionVideoListPanel() {
        val queue = viewModel.sessionQueue.value
        if (!PlayerSessionQueueChromePolicy.canOpenSessionListPanel(queue.size)) return
        showExclusivePlayerDialog { onDismiss ->
            handler.removeCallbacks(hideControlsRunnable)
            PlayerVideoListDialog(
                context = this,
                videos = queue,
                playingVideoId = viewModel.playingVideoId,
                onPick = { item ->
                    switchSessionVideo(item) {
                        currentVideoUriString = item.uri.toString()
                        currentVideoPath = item.path
                        tvTitle.text = item.title
                        loadSubtitlesAsync(playerPrefs.externalSubtitleUri.ifBlank { item.uri.toString() }, item.path)
                        applyPlayerSettings()
                        scheduleHideControls()
                    }
                }
            ).apply {
                setOnDismissListener {
                    onDismiss()
                    scheduleHideControls()
                }
            }.also { dialog ->
                dialog.show()
            }
        }
    }

    /** 与横屏齿轮按钮相同：弹出播放器设置。竖屏底栏「更多」亦指向此处。 */
    private fun openPlayerSettingsDialog() {
        showExclusivePlayerDialog { onDismiss ->
            handler.removeCallbacks(hideControlsRunnable)
            controlsVisibleBeforeSettingsOverlay = controlsVisible
            isSettingsOverlayVisible = true
            hideChromeForSettingsOverlay()
            val dialog = PlayerSettingsDialog(
                context = this,
                playerManager = playerManager,
                viewModel = viewModel,
                playerPrefs = playerPrefs,
                onScreenBrightnessChanged = ::applyScreenBrightness,
                onRequestPickSubtitle = {
                    pickSubtitleLauncher.launch(arrayOf("*/*"))
                },
                onAspectRatioChanged = ::applyDisplaySettings,
                onPlayerPrefsReset = ::applyPlayerSettings
            )
            dialog.setOnDismissListener {
                onDismiss()
                isSettingsOverlayVisible = false
                restoreChromeAfterSettingsOverlay()
                applyPlayerSettings()
                scheduleHideControls()
            }
            dialog.show()
            dialog
        }
    }

    private fun showAspectRatioQuickDialog() {
        val ratios = listOf(
            AspectRatio.FIT to R.string.player_sheet_fit_screen,
            AspectRatio.FILL to R.string.player_sheet_fill_screen,
            AspectRatio.RATIO_16_9 to R.string.settings_ratio_16_9,
            AspectRatio.RATIO_4_3 to R.string.settings_ratio_4_3,
            AspectRatio.CROP to R.string.settings_ratio_crop,
            AspectRatio.STRETCH to R.string.settings_ratio_stretch
        )
        showExclusivePlayerDialog { onDismiss ->
            PlayerGlassSheetDialog.showSingleChoice(
                context = this,
                layoutInflater = layoutInflater,
                titleRes = R.string.player_sheet_aspect_ratio,
                choices = ratios.map { (ratio, titleRes) ->
                    PlayerGlassSheetChoice(
                        value = ratio,
                        label = getString(titleRes),
                        selected = ratio == playerPrefs.aspectRatio
                    )
                },
                onDismiss = {
                    onDismiss()
                    scheduleHideControls()
                }
            ) { ratio ->
                playerPrefs.aspectRatio = ratio
                viewModel.setAspectRatio(ratio)
                applyDisplaySettings()
            }
        }
    }

    private fun showSpeedPickerDialog() {
        val speeds = DefaultPlayerSettings.supportedSpeeds
        showExclusivePlayerDialog { onDismiss ->
            PlayerGlassSheetDialog.showSingleChoice(
                context = this,
                layoutInflater = layoutInflater,
                titleRes = R.string.player_pick_speed,
                choices = speeds.map { speed ->
                    PlayerGlassSheetChoice(
                        value = speed,
                        label = "${speed}x",
                        selected = speed == playerPrefs.speed
                    )
                },
                onDismiss = {
                    onDismiss()
                    scheduleHideControls()
                }
            ) { speed ->
                playerPrefs.speed = speed
                viewModel.setSpeed(
                    speed,
                    PlayerPlaybackSettings.pitchFor(speed, playerPrefs.speedPreservePitch)
                )
                findViewById<TextView>(R.id.tv_land_speed)?.text = landSpeedLabel(speed)
            }
        }
    }

    private fun showExclusivePlayerDialog(
        showDialog: (onDismiss: () -> Unit) -> Dialog
    ) {
        val current = activePlayerDialog
        if (current?.isShowing == true) return
        var dialog: Dialog? = null
        val clearActiveDialog = {
            if (activePlayerDialog === dialog) {
                activePlayerDialog = null
            }
        }
        dialog = showDialog(clearActiveDialog)
        activePlayerDialog = dialog
    }

    /**
     * 让快速选择型 AlertDialog 继承「播放器设置」面板的不透明度与背景暗化 / 模糊，
     * 与 [PlayerSettingsDialog] 视觉一致。必须在 `dialog.show()` 之后调用。
     */
    private fun androidx.appcompat.app.AlertDialog.applyPlayerSheetStyle() {
        val w = window ?: return
        val style = PlayerSettingsSheetStylePolicy.compute(
            panelOpacityPercent = playerPrefs.settingsPanelOpacity,
            backdropDimPercent = playerPrefs.settingsSheetBackdropDimPercent,
            backdropBlurDp = playerPrefs.settingsSheetBackdropBlurDp,
            density = resources.displayMetrics.density
        )
        w.decorView.alpha = style.panelAlpha
        w.setDimAmount(style.dimAmount)
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (PlayerSettingsSheetStylePolicy.supportsBackdropBlur(Build.VERSION.SDK_INT)) {
            w.setBackgroundBlurRadius(style.backdropBlurRadiusPx)
        }
    }

    private fun showAudioTrackQuickDialog() {
        val state = PlayerQuickEntryPolicy.audioEntry(
            tracks = viewModel.audioTracks(),
            audioMuted = playerPrefs.audioMuted,
            trackLabel = { track ->
                PlayerAudioDiagnosticsPolicy.quickTrackSummary(
                    track = track,
                    streamLabel = getString(R.string.player_settings_info_stream, track.groupIndex + 1)
                )
            }
        )
        val items = state.items.map { item ->
            when (item.action) {
                PlayerQuickEntryAction.DisableAudio ->
                    item.copy(label = getString(R.string.player_sheet_disable))
                PlayerQuickEntryAction.None ->
                    item.copy(label = getString(R.string.player_settings_audio_track_none))
                else -> item
            }
        }
        showQuickEntryDialog(R.string.player_sheet_audio_track, items) { action ->
            when (action) {
                is PlayerQuickEntryAction.SelectAudioTrack -> {
                    val track = viewModel.audioTracks().firstOrNull {
                        it.groupIndex == action.groupIndex && it.trackIndex == action.trackIndex
                    } ?: return@showQuickEntryDialog
                    viewModel.selectAudioTrack(track)
                }
                PlayerQuickEntryAction.DisableAudio -> viewModel.disableAudioTrack()
                else -> Unit
            }
        }
    }

    private fun showSubtitleQuickDialog() {
        val state = PlayerQuickEntryPolicy.subtitleEntry(
            hasLoadedSubtitles = viewModel.uiState.value.subtitles.isNotEmpty(),
            subtitlesEnabled = playerPrefs.subtitlesEnabled,
            subtitleDelayMs = playerPrefs.subtitleDelayMs
        )
        val items = state.items.map { item ->
            when (val action = item.action) {
                is PlayerQuickEntryAction.SetSubtitlesEnabled ->
                    item.copy(label = getString(if (action.enabled) R.string.player_sheet_enable else R.string.settings_subtitle_track_off))
                is PlayerQuickEntryAction.SubtitleDelayStatus ->
                    item.copy(label = getString(R.string.player_quick_subtitle_delay_current, action.delayMs))
                is PlayerQuickEntryAction.AdjustSubtitleDelay ->
                    item.copy(
                        label = getString(
                            if (PlayerQuickEntryPolicy.subtitleDelayAdjustIsDecrease(action.deltaMs)) {
                                R.string.player_quick_subtitle_delay_minus_in_dialog
                            } else {
                                R.string.player_quick_subtitle_delay_plus_in_dialog
                            },
                            kotlin.math.abs(action.deltaMs),
                            playerPrefs.subtitleDelayMs
                        )
                    )
                PlayerQuickEntryAction.ResetSubtitleDelay ->
                    item.copy(label = getString(R.string.player_quick_subtitle_delay_reset))
                PlayerQuickEntryAction.PickSubtitleFile ->
                    item.copy(label = getString(R.string.player_sheet_select_subtitle_file))
                PlayerQuickEntryAction.OpenSubtitleSettings ->
                    item.copy(label = getString(R.string.player_quick_subtitle_more_settings))
                PlayerQuickEntryAction.None ->
                    item.copy(label = getString(R.string.player_quick_subtitle_none))
                else -> item
            }
        }
        showQuickEntryDialog(R.string.player_sheet_subtitles, items) { action ->
            when (action) {
                is PlayerQuickEntryAction.SetSubtitlesEnabled -> {
                    playerPrefs.subtitlesEnabled = action.enabled
                    applySubtitlePresentation()
                }
                is PlayerQuickEntryAction.AdjustSubtitleDelay -> {
                    playerPrefs.subtitleDelayMs += action.deltaMs
                    applySubtitlePresentation()
                }
                PlayerQuickEntryAction.ResetSubtitleDelay -> {
                    playerPrefs.subtitleDelayMs = 0
                    applySubtitlePresentation()
                }
                PlayerQuickEntryAction.PickSubtitleFile ->
                    pickSubtitleLauncher.launch(arrayOf("*/*"))
                PlayerQuickEntryAction.OpenSubtitleSettings ->
                    openSubtitleSettingsSheet()
                else -> Unit
            }
        }
    }

    private fun openSubtitleSettingsSheet() {
        if (supportFragmentManager.findFragmentByTag(SUBTITLE_SETTINGS_SHEET_TAG) != null) return
        handler.removeCallbacks(hideControlsRunnable)
        controlsVisibleBeforeSettingsOverlay = controlsVisible
        isSettingsOverlayVisible = true
        hideChromeForSettingsOverlay()
        PlayerSubtitleSettingsSheet().apply {
            onDismissListener = ::onSubtitleSettingsSheetDismissed
        }.show(supportFragmentManager, SUBTITLE_SETTINGS_SHEET_TAG)
    }

    private fun onSubtitleSettingsSheetDismissed() {
        if (!isSettingsOverlayVisible) return
        isSettingsOverlayVisible = false
        restoreChromeAfterSettingsOverlay()
        applyPlayerSettings()
        scheduleHideControls()
    }

    private fun dismissSubtitleSettingsSheet() {
        (supportFragmentManager.findFragmentByTag(SUBTITLE_SETTINGS_SHEET_TAG) as? PlayerSubtitleSettingsSheet)
            ?.dismissAllowingStateLoss()
    }

    private fun switchSessionVideo(item: VideoItem, onSwitched: () -> Unit = {}) {
        dismissSubtitleSettingsSheet()
        showFirstFrameScrim()
        preApplyOrientationForItem(item)
        viewModel.switchToVideo(item) {
            resetPlaybackSessionForNewVideo()
            onSwitched()
        }
    }

    private fun showQuickEntryDialog(
        titleRes: Int,
        items: List<PlayerQuickEntryItem>,
        onSelected: (PlayerQuickEntryAction) -> Unit
    ) {
        showExclusivePlayerDialog { onDismiss ->
            handler.removeCallbacks(hideControlsRunnable)
            PlayerGlassSheetDialog.showSingleChoice(
                context = this,
                layoutInflater = layoutInflater,
                titleRes = titleRes,
                choices = items.map { item ->
                    PlayerGlassSheetChoice(
                        value = item.action,
                        label = item.label,
                        selected = item.selected,
                        enabled = item.enabled
                    )
                },
                onDismiss = {
                    onDismiss()
                    scheduleHideControls()
                },
                onSelected = onSelected
            )
        }
    }

    private fun setupControls() {
        btnPlay.setPlayerClickListener(PlayerLockedInteraction.TRANSPORT) { togglePlayPauseAndSyncIcon() }
        btnBack.setPlayerClickListener(PlayerLockedInteraction.BACK) { finishPlayer() }

        btnPrev.setPlayerClickListener(PlayerLockedInteraction.TRANSPORT) { viewModel.seekBackward() }
        btnNext.setPlayerClickListener(PlayerLockedInteraction.TRANSPORT) { viewModel.seekForward() }

        btnVideoList?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            showSessionVideoListPanel()
        }

        findViewById<View>(R.id.btn_settings)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            openPlayerSettingsDialog()
        }

        findViewById<View>(R.id.portrait_btn_more)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            openPlayerSettingsDialog()
        }

        findViewById<View>(R.id.portrait_btn_speed)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            showSpeedPickerDialog()
        }

        findViewById<View>(R.id.btn_land_seek_back)?.setPlayerClickListener(PlayerLockedInteraction.TRANSPORT) {
            viewModel.seekBackward()
        }

        findViewById<View>(R.id.btn_land_seek_forward)?.setPlayerClickListener(PlayerLockedInteraction.TRANSPORT) {
            viewModel.seekForward()
        }

        findViewById<TextView>(R.id.tv_land_speed)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            showSpeedPickerDialog()
        }

        findViewById<View>(R.id.btn_land_aspect)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            showAspectRatioQuickDialog()
        }

        findViewById<View>(R.id.btn_land_subtitles)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            showSubtitleQuickDialog()
            scheduleHideControls()
        }

        findViewById<View>(R.id.btn_land_pip_float)?.setPlayerClickListener(PlayerLockedInteraction.TRANSPORT) {
            enterPipModeIfSupported()
        }

        findViewById<View>(R.id.btn_land_cast)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            Toast.makeText(this, R.string.player_land_cast, Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.portrait_btn_quality)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            showAudioTrackQuickDialog()
            scheduleHideControls()
        }

        findViewById<View>(R.id.portrait_btn_episodes)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            showSessionVideoListPanel()
        }

        findViewById<View>(R.id.portrait_btn_subtitles)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            showSubtitleQuickDialog()
            scheduleHideControls()
        }

        btnScreenshot?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            val videoView = videoRenderView()
            if (videoView != null) {
                playerManager.takeScreenshot(videoView) { success, path ->
                    runOnUiThread {
                        if (success) {
                            android.widget.Toast.makeText(this, getString(R.string.player_screenshot_saved, path), android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(this, getString(R.string.player_screenshot_failed), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        btnAbLoop?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            applyAbLoopResult(
                PlayerAbLoopPolicy.onToggle(
                    state = abLoopState,
                    pointA = abLoopPointA,
                    pointB = abLoopPointB,
                    currentPositionMs = playerManager.currentPosition
                )
            )
        }

        btnFullscreen.setPlayerClickListener(PlayerLockedInteraction.TRANSPORT) {
            userOverrodeOrientation = true
            requestedOrientation = PlayerOrientationTogglePolicy.nextRequestedOrientation(
                resources.configuration.orientation
            )
        }

        btnPip?.setPlayerClickListener(PlayerLockedInteraction.TRANSPORT) { enterPipModeIfSupported() }

        btnLock.setPlayerClickListener(PlayerLockedInteraction.LOCK_TOGGLE) {
            toggleScreenLock()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar) {
                if (!PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SEEK_BAR, isScreenLocked)) return
                isSeeking = true
                handler.removeCallbacks(hideControlsRunnable)
            }

            override fun onStopTrackingTouch(sb: SeekBar) {
                if (!PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SEEK_BAR, isScreenLocked)) return
                viewModel.seekTo(
                    PlayerTimeline.positionFromSeekBar(
                        progress = sb.progress,
                        max = sb.max,
                        durationMs = viewModel.uiState.value.duration
                    )
                )
                isSeeking = false
                scheduleHideControls()
            }
        })

        controlsContainer.setOnClickListener {
            if (!PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.CHROME_TOGGLE, isScreenLocked)) return@setOnClickListener
            if (controlsVisible) hideControls() else showControls()
        }

        playerListener?.let { viewModel.player?.removeListener(it) }
        playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon(
                    isPlaying = isPlaying,
                    playWhenReady = viewModel.player?.playWhenReady == true
                )
                startPlaybackServiceIfNeeded(isPlaying)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) unlockPlayerForPause()
                updatePlayPauseIcon(
                    isPlaying = viewModel.player?.isPlaying == true,
                    playWhenReady = playWhenReady
                )
                startPlaybackServiceIfNeeded(playWhenReady && viewModel.player?.isPlaying == true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val readyTraceUpdate = PlayerPlaybackReadyTracePolicy.onPlaybackStateChanged(
                    playbackState = playbackState,
                    wasBuffering = playbackWasBuffering,
                    hasRecordedPrepareReady = startupTrace.hasRecorded(PlayerStartupTrace.Events.PREPARE_READY)
                )
                playbackWasBuffering = readyTraceUpdate.nextWasBuffering
                when (readyTraceUpdate.readyTraceEvent) {
                    PlayerPlaybackReadyTracePolicy.ReadyTraceEvent.RECOVERED_AFTER_BUFFERING ->
                        startupTrace.record(PlayerStartupTrace.Events.READY_AFTER_BUFFERING)
                    PlayerPlaybackReadyTracePolicy.ReadyTraceEvent.FIRST_PREPARE_READY,
                    null -> Unit
                }

                if (playbackState == Player.STATE_READY) {
                    val state = viewModel.uiState.value
                    applyPlaybackTickSeek(state.currentPosition, state.duration)
                    hideFirstFrameScrimForAudioOnly()
                    onPrepareReady()
                } else if (playbackState == Player.STATE_ENDED) {
                    handlePlaybackEnded()
                }
            }

            @OptIn(UnstableApi::class)
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (PlayerVolumeBoostApplyPolicy.shouldReapplyOnAudioSessionChange(playerPrefs.volumeBoost)) {
                    viewModel.setVolumeBoost(true)
                }
            }

            override fun onRenderedFirstFrame() {
                hideFirstFrameScrim()
                cancelFirstFrameTimeoutCheck()
                if (!hasLoggedFirstFrame) {
                    hasLoggedFirstFrame = true
                    startupTrace.record(PlayerStartupTrace.Events.FIRST_FRAME_RENDERED)
                    CrashLogger.logDiagnostic(
                        this@PlayerActivity,
                        "player_startup",
                        startupTrace.format()
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                CrashLogger.logPlayerError(this@PlayerActivity, error)
                Toast.makeText(this@PlayerActivity, R.string.player_playback_error, Toast.LENGTH_SHORT).show()
            }

            @Suppress("DEPRECATION")
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                applyVideoOrientation(
                    width = videoSize.width,
                    height = videoSize.height,
                    pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio,
                    unappliedRotationDegrees = videoSize.unappliedRotationDegrees
                )
                applyPlayerContentAspectRatio(
                    width = videoSize.width,
                    height = videoSize.height,
                    pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio,
                    unappliedRotationDegrees = videoSize.unappliedRotationDegrees
                )
                applyPlayerContentFrameTransform(
                    width = videoSize.width,
                    height = videoSize.height,
                    pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio,
                    unappliedRotationDegrees = videoSize.unappliedRotationDegrees
                )
            }
        }
        viewModel.player?.addListener(playerListener!!)
        attachStartupAnalyticsListener()
        syncPlayPauseIcon()
        applyControlVisibility()
    }

    /**
     * 把 ExoPlayer 的 decoder / codec-error 事件接入 [PlayerStartupTrace]，
     * 通过 [PlayerDecoderEventPolicy] 决定每次回调要打哪几个事件名（同名事件靠
     * `recordOnce` 去重）。
     */
    @OptIn(UnstableApi::class)
    private fun attachStartupAnalyticsListener() {
        if (startupAnalyticsListener != null) return
        val listener = object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                PlayerDecoderEventPolicy.videoDecoderEvents(decoderName)
                    .forEach { startupTrace.recordOnce(it) }
            }

            override fun onAudioDecoderInitialized(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                PlayerDecoderEventPolicy.audioDecoderEvents(decoderName)
                    .forEach { startupTrace.recordOnce(it) }
            }

            override fun onVideoCodecError(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                videoCodecError: Exception
            ) {
                PlayerDecoderEventPolicy.videoCodecErrorEvents(videoCodecError.javaClass.name)
                    .forEach { startupTrace.recordOnce(it) }
            }

            override fun onAudioCodecError(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                audioCodecError: Exception
            ) {
                PlayerDecoderEventPolicy.audioCodecErrorEvents(audioCodecError.javaClass.name)
                    .forEach { startupTrace.recordOnce(it) }
            }
        }
        viewModel.player?.addAnalyticsListener(listener)
        startupAnalyticsListener = listener
    }

    private fun applyAbLoopResult(result: PlayerAbLoopResult) {
        abLoopState = result.state
        abLoopPointA = result.pointA
        abLoopPointB = result.pointB

        when (result.event) {
            PlayerAbLoopEvent.POINT_A_SET -> {
                if (PlayerAbLoopButtonStylePolicy.shouldHighlight(result.event)) {
                    btnAbLoop?.setColorFilter(ContextCompat.getColor(this, R.color.player_accent))
                }
                android.widget.Toast.makeText(
                    this,
                    getString(R.string.player_ab_point_a_set, formatTime(abLoopPointA)),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            PlayerAbLoopEvent.LOOP_STARTED -> {
                if (PlayerAbLoopButtonStylePolicy.shouldHighlight(result.event)) {
                    btnAbLoop?.setColorFilter(ContextCompat.getColor(this, R.color.player_accent))
                }
                android.widget.Toast.makeText(this, getString(R.string.player_ab_loop_started), android.widget.Toast.LENGTH_SHORT).show()
            }
            PlayerAbLoopEvent.INVALID_POINT_B -> {
                if (PlayerAbLoopButtonStylePolicy.shouldClearHighlight(result.event)) {
                    btnAbLoop?.clearColorFilter()
                }
                android.widget.Toast.makeText(this, getString(R.string.player_ab_point_b_error), android.widget.Toast.LENGTH_SHORT).show()
            }
            PlayerAbLoopEvent.CANCELLED -> {
                if (PlayerAbLoopButtonStylePolicy.shouldClearHighlight(result.event)) {
                    btnAbLoop?.clearColorFilter()
                }
                android.widget.Toast.makeText(this, getString(R.string.player_ab_loop_cancelled), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFirstFrameScrim() {
        applyFirstFrameDecision(PlayerFirstFramePolicy.onShowForNewMedia())
    }

    private fun hideFirstFrameScrim() {
        applyFirstFrameDecision(
            PlayerFirstFramePolicy.onRenderedFirstFrame(
                isAwaitingFirstFrame = isAwaitingFirstFrame
            )
        )
    }

    private fun hideFirstFrameScrimForAudioOnly() {
        applyFirstFrameDecision(
            PlayerFirstFramePolicy.onReady(
                isAwaitingFirstFrame = isAwaitingFirstFrame,
                hasVideoTrack = hasVideoTrack()
            )
        )
    }

    /**
     * 第一次 `STATE_READY` 到达后打 `prepare_ready` 埋点并排程「首帧迟滞」检查。
     */
    private fun onPrepareReady() {
        if (startupTrace.hasRecorded(PlayerStartupTrace.Events.PREPARE_READY)) return
        startupTrace.recordOnce(PlayerStartupTrace.Events.PREPARE_READY)
        scheduleFirstFrameTimeoutCheck()
    }

    private fun scheduleFirstFrameTimeoutCheck() {
        val delayMs = PlayerFirstFrameTimeoutPolicy.scheduleDelayMs(
            hasVideoTrack = hasVideoTrack(),
            firstFrameRendered = hasLoggedFirstFrame,
            alreadyTimedOut = hasLoggedFirstFrameTimeout
        ) ?: return
        cancelFirstFrameTimeoutCheck()
        handler.postDelayed(firstFrameTimeoutRunnable, delayMs)
        isFirstFrameTimeoutPosted = true
    }

    private fun cancelFirstFrameTimeoutCheck() {
        if (!isFirstFrameTimeoutPosted) return
        handler.removeCallbacks(firstFrameTimeoutRunnable)
        isFirstFrameTimeoutPosted = false
    }

    private val firstFrameTimeoutRunnable = Runnable {
        isFirstFrameTimeoutPosted = false
        if (hasLoggedFirstFrame || hasLoggedFirstFrameTimeout) return@Runnable
        hasLoggedFirstFrameTimeout = true
        startupTrace.recordOnce(PlayerStartupTrace.Events.FIRST_FRAME_TIMEOUT)
        CrashLogger.logDiagnostic(this, "player_first_frame_timeout", startupTrace.format())
    }

    private fun applyFirstFrameDecision(decision: PlayerFirstFrameDecision) {
        isAwaitingFirstFrame = decision.nextAwaitingFirstFrame
        if (!this::firstFrameScrim.isInitialized) return

        val presentation = PlayerFirstFrameScrimPolicy.presentation(decision) ?: return
        firstFrameScrim.animate().cancel()
        firstFrameScrim.alpha = presentation.alpha
        firstFrameScrim.visibility = if (presentation.visible) View.VISIBLE else View.GONE
    }

    private fun handlePlaybackEnded() {
        if (isSwitchingQueueAfterEnded) return
        val queue = viewModel.sessionQueue.value
        val currentIndex = queue.indexOfFirst { it.id == viewModel.playingVideoId }
        val decision = PlayerPlaybackEndPolicy.decide(
            currentIndex = currentIndex,
            queueSize = queue.size,
            autoPlayNext = playerPrefs.autoPlayNext,
            loopMode = playerPrefs.loopMode,
            abLoopState = abLoopState,
            abLoopPointA = abLoopPointA,
            endBehavior = playerPrefs.playbackEndBehavior
        )

        when (decision.action) {
            PlayerPlaybackEndAction.PLAY_NEXT -> playNextQueueVideoAfterEnded(queue, decision.nextIndex)
            PlayerPlaybackEndAction.REPLAY_CURRENT -> {
                viewModel.seekTo(decision.seekPositionMs ?: 0L)
                viewModel.player?.play()
                scheduleHideControls()
            }
            PlayerPlaybackEndAction.STOP_AT_END -> {
                viewModel.saveHistory()
                showControls()
            }
            PlayerPlaybackEndAction.RETURN_TO_LIST -> {
                viewModel.saveHistory()
                finishPlayer()
            }
        }
    }

    private fun playNextQueueVideoAfterEnded(queue: List<VideoItem>, nextIndex: Int?) {
        if (nextIndex == null) return

        isSwitchingQueueAfterEnded = true
        switchSessionVideo(queue[nextIndex]) {
            isSwitchingQueueAfterEnded = false
            currentVideoUriString = queue[nextIndex].uri.toString()
            currentVideoPath = queue[nextIndex].path
            tvTitle.text = queue[nextIndex].title
            loadSubtitlesAsync(
                playerPrefs.externalSubtitleUri.ifBlank { queue[nextIndex].uri.toString() },
                queue[nextIndex].path
            )
            applyPlayerSettings()
            scheduleHideControls()
        }
    }

    private fun resetPlaybackSessionForNewVideo() {
        val reset = PlayerVideoSwitchPolicy.resetForNewVideo()
        hasSkippedIntro = reset.hasSkippedIntro
        hasSkippedOutro = reset.hasSkippedOutro
        abLoopState = reset.abLoopState
        abLoopPointA = reset.abLoopPointA
        abLoopPointB = reset.abLoopPointB
        pendingSeekTarget = reset.pendingSeekTarget
        seekGestureAnchorPositionMs = reset.seekGestureAnchorPositionMs
        doubleTapSeekState = reset.doubleTapSeekState
        doubleTapSeekAnchorPositionMs = reset.doubleTapSeekAnchorPositionMs
        keepGestureHudAfterActionUp = reset.keepGestureHudAfterActionUp
        isAwaitingFirstFrame = reset.awaitFirstFrame
        manualVideoZoom = reset.manualVideoZoom
        playbackWasBuffering = false
        lastHistorySavedPositionMs = PlaybackProgressPolicy.onNewMedia()
        btnAbLoop?.clearColorFilter()
        hideGestureHud()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initGestures() {
        pinchZoomDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    if (!PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio)) return false
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    manualVideoZoom = manualVideoZoom.copy(
                        scale = PlayerVideoZoomPolicy.applyScaleFactor(
                            currentScale = manualVideoZoom.scale,
                            scaleFactor = detector.scaleFactor
                        )
                    )
                    applyPlayerContentFrameTransform()
                    showGestureHud(PlayerGestureHudPolicy.zoom(manualVideoZoom.scale), autoHide = false)
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    handler.postDelayed(hideGestureHudRunnable, PlayerGestureHudDisplayPolicy.AUTO_HIDE_DELAY_MS)
                }
            }
        )

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isScreenLocked) {
                    if (PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.REVEAL_LOCKED_CHROME, isScreenLocked)) {
                        showLockedControls()
                    }
                    return true
                }
                if (controlsVisible) hideControls() else showControls()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.GESTURE_PLAYBACK, isScreenLocked)) {
                    return true
                }
                if (PlayerVideoZoomGesturePolicy.doubleTapResetsZoom(
                        zoomAllowed = PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio),
                        manual = manualVideoZoom
                    )
                ) {
                    resetManualVideoZoom(showHud = true)
                    return true
                }
                when (playerPrefs.doubleTapAction) {
                    DoubleTapAction.PLAY_PAUSE -> viewModel.togglePlayPause()
                    DoubleTapAction.FORWARD -> handleDoubleTapSeek(PlayerSwipeSide.RIGHT)
                    DoubleTapAction.BACKWARD -> handleDoubleTapSeek(PlayerSwipeSide.LEFT)
                    DoubleTapAction.NONE -> {}
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.GESTURE_PLAYBACK, isScreenLocked)) {
                    return
                }
                val decision = PlayerLongPressPolicy.onPress(
                    action = playerPrefs.longPressAction,
                    requestedSpeed = playerPrefs.longPressSpeed,
                    restoreSpeed = playerPrefs.speed
                )
                isLongPressing = decision.startLongPress
                decision.targetSpeed?.let { speed ->
                    viewModel.setSpeed(speed)
                    showGestureHud(PlayerGestureHudPolicy.speed(speed), autoHide = false)
                }
            }
        })

        var startX = 0f
        var startY = 0f
        var isHorizontalSwipe = false
        var isVerticalSwipe = false
        var isEdgeSwipe = false
        var swipeSide = PlayerSwipeSide.NONE
        var zoomPanStartX = 0f
        var zoomPanStartY = 0f
        var zoomPanAnchorX = 0f
        var zoomPanAnchorY = 0f
        var zoomPanMoved = false
        var verticalLevelGestureAllowed = false
        val gestureSlop = gestureSlopPx()
        val zoomAllowed = { PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio) }

        gestureOverlay.setOnTouchListener { _, event ->
            if (isScreenLocked) {
                val decision = PlayerLockGesturePolicy.onTouch(
                    isLocked = true,
                    action = PlayerTouchActionPolicy.fromMotionActionMasked(event.actionMasked)
                )
                if (decision.revealLockedControls) showLockedControls()
                return@setOnTouchListener decision.consumeTouch
            }

            if (zoomAllowed()) {
                pinchZoomDetector.onTouchEvent(event)
            }
            if (zoomAllowed() && (event.pointerCount >= 2 || pinchZoomDetector.isInProgress)) {
                if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                    isHorizontalSwipe = false
                    isVerticalSwipe = false
                    pendingSeekTarget = null
                    seekGestureAnchorPositionMs = null
                }
                return@setOnTouchListener true
            }

            if (PlayerVideoZoomGesturePolicy.interceptsSingleFingerGestures(zoomAllowed(), manualVideoZoom)) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        zoomPanStartX = event.x
                        zoomPanStartY = event.y
                        zoomPanAnchorX = manualVideoZoom.panX
                        zoomPanAnchorY = manualVideoZoom.panY
                        zoomPanMoved = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - zoomPanStartX
                        val dy = event.y - zoomPanStartY
                        if (abs(dx) > gestureSlop || abs(dy) > gestureSlop) {
                            zoomPanMoved = true
                            val (panX, panY) = PlayerVideoZoomPolicy.panFromDrag(
                                anchorPanX = zoomPanAnchorX,
                                anchorPanY = zoomPanAnchorY,
                                dragDx = dx,
                                dragDy = dy,
                                baseScale = cachedBaseContentFrameTransform.scale,
                                manualScale = manualVideoZoom.scale,
                                frameWidth = playerView.width,
                                frameHeight = playerView.height,
                                viewportWidth = playerView.width,
                                viewportHeight = playerView.height
                            )
                            manualVideoZoom = manualVideoZoom.copy(panX = panX, panY = panY)
                            applyPlayerContentFrameTransform()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!zoomPanMoved) {
                            gestureDetector.onTouchEvent(event)
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> Unit
                }
                return@setOnTouchListener true
            }

            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isHorizontalSwipe = false
                    isVerticalSwipe = false
                    brightnessGestureAnchor = currentBrightness
                    volumeGestureAnchor = currentVolume
                    pendingSeekTarget = null
                    seekGestureAnchorPositionMs = viewModel.uiState.value.currentPosition
                    isEdgeSwipe = PlayerGesturePolicy.isEdgeSwipe(event.x, resources.displayMetrics.widthPixels)
                    swipeSide = PlayerGesturePolicy.swipeSide(event.x, resources.displayMetrics.widthPixels)
                    verticalLevelGestureAllowed = PlayerGesturePolicy.allowsVerticalLevelGesture(
                        yPx = event.y,
                        screenHeightPx = resources.displayMetrics.heightPixels
                    )
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - startX
                    val dy = event.y - startY

                    if (!isHorizontalSwipe && !isVerticalSwipe) {
                        when (PlayerGesturePolicy.dominantAxis(dx, dy, gestureSlop)) {
                            PlayerSwipeAxis.HORIZONTAL -> isHorizontalSwipe = true
                            PlayerSwipeAxis.VERTICAL -> {
                                if (verticalLevelGestureAllowed) {
                                    isVerticalSwipe = true
                                }
                            }
                            PlayerSwipeAxis.NONE -> {}
                        }
                    }

                    if (isHorizontalSwipe) {
                        handleHorizontalSwipeAction(dx)
                    } else if (isVerticalSwipe) {
                        val action = resolveVerticalGestureAction(swipeSide)
                        when (action) {
                            GestureAction.BRIGHTNESS -> handleBrightnessGesture(dy)
                            GestureAction.VOLUME -> handleVolumeGesture(dy)
                            GestureAction.SEEK -> handleVerticalSeekGesture(dy)
                            GestureAction.NONE -> {}
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // P1: Edge swipe back
                    val dx = event.x - startX
                    if (PlayerEdgeSwipeBackPolicy.shouldFinish(
                            edgeSwipeBackEnabled = playerPrefs.edgeSwipeBack,
                            isEdgeSwipe = isEdgeSwipe,
                            isHorizontalSwipe = isHorizontalSwipe,
                            dragDxPx = dx
                        )
                    ) {
                        finishPlayer()
                        return@setOnTouchListener true
                    }

                    if (PlayerGesturePolicy.shouldApplyHorizontalSeekOnRelease(
                            isHorizontalSwipe = isHorizontalSwipe,
                            horizontalSwipeAction = playerPrefs.horizontalSwipeAction
                        )
                    ) {
                        applySeekGesture()
                    } else if (isVerticalSwipe) {
                        val verticalAction = resolveVerticalGestureAction(swipeSide)
                        if (PlayerGesturePolicy.shouldApplyVerticalSeekOnRelease(isVerticalSwipe, verticalAction)) {
                            applySeekGesture()
                        }
                    }
                    if (keepGestureHudAfterActionUp) {
                        keepGestureHudAfterActionUp = false
                    } else {
                        hideGestureHud()
                    }

                    // P1: Restore speed after long press
                    releaseLongPressSpeed()
                    seekGestureAnchorPositionMs = null
                }
                MotionEvent.ACTION_CANCEL -> {
                    pendingSeekTarget = null
                    seekGestureAnchorPositionMs = null
                    hideGestureHud()
                    releaseLongPressSpeed()
                }
            }
            true
        }
    }

    private fun resetManualVideoZoom(showHud: Boolean = false) {
        manualVideoZoom = PlayerVideoZoomState.IDENTITY
        applyPlayerContentFrameTransform()
        if (showHud) {
            showGestureHud(PlayerGestureHudPolicy.zoom(PlayerVideoZoomPolicy.MIN_SCALE))
        }
    }

    private fun releaseLongPressSpeed() {
        val release = PlayerLongPressPolicy.onRelease(
            isLongPressing = isLongPressing,
            restoreSpeed = playerPrefs.speed
        )
        isLongPressing = false
        release.restoreSpeed?.let(viewModel::setSpeed)
    }

    private fun gestureSlopPx(): Int =
        PlayerGesturePolicy.gestureSlopPx(playerPrefs.gestureSensitivity)

    private fun handleHorizontalSwipeAction(dx: Float) {
        PlayerGestureDispatchPolicy.onHorizontalSwipe(playerPrefs.horizontalSwipeAction) {
            handleSeekGesture(dx)
        }
    }

    private fun resolveVerticalGestureAction(side: PlayerSwipeSide): GestureAction =
        PlayerGesturePolicy.verticalGestureAction(
            side = side,
            leftAction = playerPrefs.leftVerticalGesture,
            rightAction = playerPrefs.rightVerticalGesture
        )

    private fun handleDoubleTapSeek(side: PlayerSwipeSide) {
        if (!PlayerGesturePolicy.isValidDoubleTapSeekSide(side)) return

        val intervalMs = PlayerDoubleTapSeekPolicy.intervalMs(playerPrefs.seekInterval)
        val state = viewModel.uiState.value
        val preview = PlayerDoubleTapSeekPolicy.preview(
            previous = doubleTapSeekState,
            tapSide = side,
            intervalMs = intervalMs,
            nowMs = SystemClock.uptimeMillis(),
            anchorPositionMs = doubleTapSeekAnchorPositionMs,
            currentPositionMs = state.currentPosition,
            durationMs = state.duration
        )
        doubleTapSeekState = preview.nextState
        keepGestureHudAfterActionUp = true
        doubleTapSeekAnchorPositionMs = preview.anchorPositionMs

        if (preview.seekable) {
            viewModel.seekTo(preview.targetMs)
        }
        showGestureHud(PlayerGestureHudPolicy.seek(preview.targetMs, state.duration, preview.deltaMs))
    }

    private fun handleSeekGesture(dx: Float) {
        val state = viewModel.uiState.value
        val preview = PlayerSeekGesturePolicy.horizontalPreview(
            anchorPositionMs = seekGestureAnchorPositionMs ?: state.currentPosition,
            dx = dx,
            screenWidthPx = resources.displayMetrics.widthPixels,
            durationMs = state.duration,
            sensitivity = playerPrefs.gestureSensitivity
        )
        pendingSeekTarget = preview.targetMs.takeIf { preview.seekable }
        showGestureHud(PlayerGestureHudPolicy.seek(preview.targetMs, state.duration, preview.deltaMs), autoHide = false)
    }

    private fun handleVerticalSeekGesture(dy: Float) {
        val state = viewModel.uiState.value
        val preview = PlayerSeekGesturePolicy.verticalPreview(
            anchorPositionMs = seekGestureAnchorPositionMs ?: state.currentPosition,
            dy = dy,
            screenHeightPx = resources.displayMetrics.heightPixels,
            durationMs = state.duration,
            sensitivity = playerPrefs.gestureSensitivity
        )
        pendingSeekTarget = preview.targetMs.takeIf { preview.seekable }
        showGestureHud(PlayerGestureHudPolicy.seek(preview.targetMs, state.duration, preview.deltaMs), autoHide = false)
    }

    private fun applySeekGesture() {
        val state = viewModel.uiState.value
        // Re-read the target from the indicator text isn't reliable, recalculate
        // The target was computed in handleSeekGesture, store it instead
        pendingSeekTarget?.let { target ->
            viewModel.seekTo(PlayerTimeline.safeSeekTarget(0, target, state.duration))
            pendingSeekTarget = null
            seekGestureAnchorPositionMs = null
        }
    }

    private fun showGestureHud(hud: PlayerGestureHud, autoHide: Boolean = true) {
        handler.removeCallbacks(hideGestureHudRunnable)
        seekIndicator.text = PlayerGestureHudDisplayPolicy.indicatorText(hud)
        seekIndicator.visibility = View.VISIBLE
        if (autoHide) {
            handler.postDelayed(hideGestureHudRunnable, PlayerGestureHudDisplayPolicy.AUTO_HIDE_DELAY_MS)
        }
    }

    private fun hideGestureHud() {
        handler.removeCallbacks(hideGestureHudRunnable)
        seekIndicator.visibility = View.GONE
        brightnessIndicator.visibility = View.GONE
        volumeIndicator.visibility = View.GONE
    }

    private fun handleBrightnessGesture(dy: Float) {
        val adjustment = PlayerLevelAdjustmentPolicy.verticalBrightness(
            anchor = brightnessGestureAnchor,
            dy = dy,
            screenHeightPx = resources.displayMetrics.heightPixels
        )
        currentBrightness = adjustment.level
        setWindowBrightness(currentBrightness)
        brightnessProgress.progress = adjustment.progressPercent
        brightnessIndicator.visibility = View.VISIBLE
        showGestureHud(PlayerGestureHudPolicy.level(PlayerGestureHudKind.BRIGHTNESS, currentBrightness), autoHide = false)
    }

    private fun handleBrightnessGestureHorizontal(dx: Float) {
        val adjustment = PlayerLevelAdjustmentPolicy.horizontalBrightness(
            anchor = brightnessGestureAnchor,
            dx = dx,
            screenWidthPx = resources.displayMetrics.widthPixels
        )
        currentBrightness = adjustment.level
        setWindowBrightness(currentBrightness)
        brightnessProgress.progress = adjustment.progressPercent
        brightnessIndicator.visibility = View.VISIBLE
        showGestureHud(PlayerGestureHudPolicy.level(PlayerGestureHudKind.BRIGHTNESS, currentBrightness), autoHide = false)
    }

    private fun applyScreenBrightness(adjustmentPercent: Int) {
        val brightness = PlayerDisplayAdjustment.screenBrightnessFor(adjustmentPercent)
        setWindowBrightness(brightness)
        if (brightness >= 0f) {
            currentBrightness = brightness
            if (this::brightnessProgress.isInitialized) {
                brightnessProgress.progress = PlayerWindowBrightnessPolicy.levelToProgressPercent(brightness)
            }
        }
    }

    /**
     * 按屏宽/屏高比例微调横屏控件边距与运输区按钮尺寸（对齐 design/横屏播放界面 稿）。
     */
    private fun applyLandscapePlayerGeometry() {
        if (!PlayerConfigurationOrientationPolicy.isLandscape(resources.configuration.orientation)) return
        val geometry = PlayerLandscapeGeometryPolicy.compute(
            widthPx = controlsContainer.width,
            heightPx = controlsContainer.height,
            density = resources.displayMetrics.density
        ) ?: return

        topBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = geometry.containerHorizontalMarginPx
            marginEnd = geometry.containerHorizontalMarginPx
            topMargin = geometry.topBarTopMarginPx
        }
        bottomPanel.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = geometry.containerHorizontalMarginPx
            marginEnd = geometry.containerHorizontalMarginPx
            bottomMargin = geometry.bottomPanelBottomMarginPx
        }
        btnLock.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginStart = geometry.lockMarginStartPx
            topMargin = 0
            bottomMargin = 0
            verticalBias = 0.5f
        }
        landRightFloatColumn?.updateLayoutParams<ConstraintLayout.LayoutParams> {
            marginEnd = geometry.containerHorizontalMarginPx
        }

        listOf(
            R.id.btn_land_seek_back,
            R.id.btn_prev,
            R.id.btn_next,
            R.id.btn_land_seek_forward
        ).forEach { id ->
            findViewById<View>(id)?.updateLayoutParams<LinearLayout.LayoutParams> {
                width = geometry.iconSidePx
                height = geometry.iconSidePx
            }
        }
        findViewById<View>(R.id.btn_fullscreen)?.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = geometry.iconSidePx
            height = geometry.iconSidePx
        }
        findViewById<View>(R.id.btn_play)?.updateLayoutParams<LinearLayout.LayoutParams> {
            width = geometry.playSidePx
            height = geometry.playSidePx
            marginStart = geometry.transportGapPx
            marginEnd = geometry.transportGapPx
        }
        findViewById<View>(R.id.btn_prev)?.updateLayoutParams<LinearLayout.LayoutParams> {
            marginStart = geometry.innerGapPx
        }
        findViewById<View>(R.id.btn_next)?.updateLayoutParams<LinearLayout.LayoutParams> {
            marginEnd = geometry.innerGapPx
        }
    }

    private fun currentTrackGroupTypes(): List<Int> =
        viewModel.player?.currentTracks?.groups?.map { it.type } ?: emptyList()

    private fun hasVideoTrack(): Boolean =
        PlayerMediaTracksPolicy.hasVideoTrack(currentTrackGroupTypes(), C.TRACK_TYPE_VIDEO)

    private fun setWindowBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
    }

    private fun handleVolumeGesture(dy: Float) {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val adjustment = PlayerLevelAdjustmentPolicy.verticalVolume(
            anchor = volumeGestureAnchor,
            dy = dy,
            screenHeightPx = resources.displayMetrics.heightPixels,
            maxVolume = maxVolume
        )
        currentVolume = adjustment.level
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, adjustment.streamVolume ?: 0, 0)
        volumeProgress.progress = adjustment.progressPercent
        volumeIndicator.visibility = View.VISIBLE
        showGestureHud(PlayerGestureHudPolicy.level(PlayerGestureHudKind.VOLUME, currentVolume), autoHide = false)
    }

    private fun handleVolumeGestureHorizontal(dx: Float) {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val adjustment = PlayerLevelAdjustmentPolicy.horizontalVolume(
            anchor = volumeGestureAnchor,
            dx = dx,
            screenWidthPx = resources.displayMetrics.widthPixels,
            maxVolume = maxVolume
        )
        currentVolume = adjustment.level
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, adjustment.streamVolume ?: 0, 0)
        volumeProgress.progress = adjustment.progressPercent
        volumeIndicator.visibility = View.VISIBLE
        showGestureHud(PlayerGestureHudPolicy.level(PlayerGestureHudKind.VOLUME, currentVolume), autoHide = false)
    }

    private fun observeState() {
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    private fun stopObservingState() {
        handler.removeCallbacks(updateRunnable)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean, playWhenReady: Boolean) {
        val icon = PlayerPlayPausePolicy.iconFor(
            isPlaying = isPlaying,
            playWhenReady = playWhenReady
        )
        btnPlay.setImageResource(icon)
    }

    private fun togglePlayPauseAndSyncIcon() {
        val decision = PlayerPlayPausePolicy.toggleDecision(
            isPlaying = viewModel.player?.isPlaying == true,
            playWhenReady = viewModel.player?.playWhenReady == true
        )
        viewModel.togglePlayPause()
        btnPlay.setImageResource(decision.iconRes)
    }

    private fun syncPlayPauseIcon() {
        val icon = PlayerPlayPausePolicy.iconFor(
            isPlaying = viewModel.player?.isPlaying == true,
            playWhenReady = viewModel.player?.playWhenReady == true
        )
        btnPlay.setImageResource(icon)
    }

    private fun saveProgressPeriodically(positionMs: Long) {
        val decision = PlaybackProgressPolicy.onPositionTick(
            positionMs = positionMs,
            lastSavedPositionMs = lastHistorySavedPositionMs
        )
        lastHistorySavedPositionMs = decision.nextLastSavedPositionMs
        if (decision.shouldSaveHistory) viewModel.saveHistory()
    }

    private fun applySubtitlePresentation() {
        val subtitleText = PlayerSubtitlePresentationPolicy.resolveSubtitleText(
            subtitlesEnabled = playerPrefs.subtitlesEnabled,
            currentSubtitle = viewModel.getCurrentSubtitle()
        )
        val presentation = PlayerSubtitlePresentationPolicy.present(
            subtitlesEnabled = playerPrefs.subtitlesEnabled,
            subtitleText = subtitleText
        )
        tvSubtitle.text = presentation.text
        tvSubtitle.visibility = if (presentation.visible) View.VISIBLE else View.GONE
    }

    private fun controlsChromeMaxAlpha(): Float =
        PlayerChromePolicy.maxChromeAlpha(playerPrefs.controlsOpacity)

    private fun showControls() {
        applyChromePresentation(
            PlayerChromeVisibilityPolicy.show(
                controlsOpacityPercent = playerPrefs.controlsOpacity,
                autoHideSeconds = playerPrefs.controlsAutoHide
            )
        )
        applyControlVisibility()
    }

    private fun hideControls() {
        val presentation = PlayerChromeVisibilityPolicy.hide()
        controlsVisible = presentation.controlsVisible
        controlsContainer.animate().cancel()
        controlsContainer.animate().alpha(presentation.alpha)
            .setDuration(PlayerChromePolicy.CHROME_FADE_DURATION_MS).withEndAction {
            if (!controlsVisible) {
                controlsContainer.visibility = if (presentation.containerVisible) View.VISIBLE else View.GONE
            }
        }.start()
    }

    private fun showLockedControls() {
        applyChromePresentation(
            PlayerChromeVisibilityPolicy.showLocked(
                controlsOpacityPercent = playerPrefs.controlsOpacity
            )
        )
        applyControlVisibility()
    }

    private fun applyControlVisibility() {
        if (PlayerChromeSettingsOverlayPolicy.hidesAllChromeRegions(isSettingsOverlayVisible)) {
            topScrim.visibility = View.GONE
            bottomScrim.visibility = View.GONE
            topBar.visibility = View.GONE
            bottomPanel.visibility = View.GONE
            toolRow.visibility = View.GONE
            landRightFloatColumn?.visibility = View.GONE
            btnLock.visibility = View.GONE
            btnFullscreen.visibility = View.GONE
            return
        }
        val visibility = PlayerLockedControlsPolicy.visibility(isScreenLocked, controlsVisible)
        fun regionVisibility(region: PlayerChromeRegion): Int =
            if (PlayerLockedControlsPolicy.isChromeRegionVisible(region, isScreenLocked, controlsVisible)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        topScrim.visibility = regionVisibility(PlayerChromeRegion.TOP_SCRIM)
        bottomScrim.visibility = regionVisibility(PlayerChromeRegion.BOTTOM_SCRIM)
        topBar.visibility = regionVisibility(PlayerChromeRegion.TOP_BAR)
        bottomPanel.visibility = regionVisibility(PlayerChromeRegion.BOTTOM_PANEL)
        toolRow.visibility = regionVisibility(PlayerChromeRegion.TOOL_ROW)
        landRightFloatColumn?.visibility = regionVisibility(PlayerChromeRegion.LAND_RIGHT_FLOAT_COLUMN)
        btnLock.visibility = if (visibility.lockButtonVisible) View.VISIBLE else View.GONE
        btnLock.isSelected = visibility.lockButtonSelected
        if (PlayerLockButtonStylePolicy.shouldUseAccentTint(visibility.lockButtonSelected)) {
            btnLock.setColorFilter(ContextCompat.getColor(this, R.color.player_accent))
        } else {
            btnLock.clearColorFilter()
        }
        btnFullscreen.visibility =
            if (visibility.fullscreenButtonVisible) View.VISIBLE else View.GONE
    }

    private fun View.setPlayerClickListener(interaction: PlayerLockedInteraction, onClick: () -> Unit) {
        setOnClickListener {
            if (!PlayerLockedControlsPolicy.allows(interaction, isScreenLocked)) return@setOnClickListener
            onClick()
        }
    }

    private fun hideChromeForSettingsOverlay() {
        controlsContainer.animate().cancel()
        controlsContainer.alpha = 0f
        controlsContainer.visibility = View.GONE
        applyControlVisibility()
    }

    private fun restoreChromeAfterSettingsOverlay() {
        controlsVisible = controlsVisibleBeforeSettingsOverlay
        controlsContainer.animate().cancel()
        controlsContainer.alpha = PlayerChromeSettingsOverlayPolicy.restoreContainerAlpha(
            controlsWereVisible = controlsVisibleBeforeSettingsOverlay,
            maxAlpha = controlsChromeMaxAlpha()
        )
        controlsContainer.visibility =
            if (PlayerChromeSettingsOverlayPolicy.restoreContainerVisible(controlsVisibleBeforeSettingsOverlay)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        applyControlVisibility()
    }

    private fun scheduleHideControls() {
        if (PlayerChromeSettingsOverlayPolicy.suppressesControlAutoHide(isSettingsOverlayVisible)) return
        handler.removeCallbacks(hideControlsRunnable)
        val presentation = PlayerChromeVisibilityPolicy.show(
            controlsOpacityPercent = playerPrefs.controlsOpacity,
            autoHideSeconds = playerPrefs.controlsAutoHide
        )
        presentation.hideDelayMs?.let { delay ->
            handler.postDelayed(hideControlsRunnable, delay)
        }
    }

    private fun applyChromePresentation(presentation: PlayerChromePresentation) {
        controlsVisible = presentation.controlsVisible
        controlsContainer.animate().cancel()
        controlsContainer.alpha = presentation.alpha
        controlsContainer.visibility = if (presentation.containerVisible) View.VISIBLE else View.GONE
        handler.removeCallbacks(hideControlsRunnable)
        presentation.hideDelayMs?.let { delay ->
            handler.postDelayed(hideControlsRunnable, delay)
        }
    }

    private fun applyVideoOrientation(
        width: Int,
        height: Int,
        pixelWidthHeightRatio: Float = 1f,
        unappliedRotationDegrees: Int = 0
    ) {
        if (!PlayerVideoOrientationApplyPolicy.shouldApply(
                autoOrientationByVideo = playerPrefs.autoOrientationByVideo,
                userOverrodeOrientation = userOverrodeOrientation,
                width = width,
                height = height
            )
        ) {
            return
        }
        val targetOrientation = PlayerVideoLayoutPolicy.orientationForVideo(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
        if (requestedOrientation == targetOrientation) return
        requestedOrientation = targetOrientation
    }

    private fun applyInitialVideoOrientation() {
        val (width, height) = resolveInitialVideoDimensions()
        requestedOrientation = PlayerOrientationPolicy.initialOrientationForVideo(
            width = width,
            height = height,
            autoOrientationByVideo = playerPrefs.autoOrientationByVideo
        )
    }

    private fun resolveInitialVideoDimensions(): Pair<Int, Int> {
        var width = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0)
        var height = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0)
        if (width > 0 && height > 0) return width to height

        val videoId = intent.getLongExtra("video_id", 0L)
        if (videoId != 0L) {
            intent.sessionVideoQueue().firstOrNull { it.id == videoId }?.let { item ->
                if (item.width > 0 && item.height > 0) {
                    return item.width to item.height
                }
            }
        }
        return width to height
    }

    /**
     * 切换视频前，用 MediaStore 已记录的宽高预设方向，避免「先横屏→再竖屏」过渡。
     * ExoPlayer 解码完成后 `onVideoSizeChanged` 会用精确尺寸再次校正。
     * 新视频开始播放时复位用户手动方向标志，让自动方向重新生效。
     */
    private fun preApplyOrientationForItem(item: VideoItem) {
        userOverrodeOrientation = false
        applyVideoOrientation(width = item.width, height = item.height)
    }

    private fun toggleScreenLock() {
        isScreenLocked = !isScreenLocked
        if (isScreenLocked) {
            setLockedGestureOverlay()
            applyScreenLockChromeReveal()
            android.widget.Toast.makeText(this, getString(R.string.player_locked), android.widget.Toast.LENGTH_SHORT).show()
        } else {
            initGestures()
            applyScreenLockChromeReveal()
            scheduleHideControls()
            android.widget.Toast.makeText(this, getString(R.string.player_unlocked), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyScreenLockChromeReveal() {
        val reveal = PlayerScreenLockChromePolicy.revealChrome(controlsChromeMaxAlpha())
        controlsVisible = reveal.controlsVisible
        controlsContainer.visibility = if (reveal.containerVisible) View.VISIBLE else View.GONE
        controlsContainer.alpha = reveal.alpha
        applyControlVisibility()
    }

    private fun setLockedGestureOverlay() {
        gestureOverlay.setOnTouchListener { _, event ->
            val decision = PlayerLockGesturePolicy.onTouch(
                isLocked = isScreenLocked,
                action = PlayerTouchActionPolicy.fromMotionActionMasked(event.actionMasked)
            )
            if (decision.revealLockedControls) showLockedControls()
            decision.consumeTouch
        }
    }

    private fun unlockPlayerForPause() {
        if (!isScreenLocked) return
        isScreenLocked = false
        initGestures()
        controlsContainer.animate().cancel()
        applyScreenLockChromeReveal()
        scheduleHideControls()
    }

    private fun finishPlayer() {
        val decision = PlayerExitPolicy.requestFinish(exitState)
        val presentation = PlayerExitPolicy.finishPresentation()
        exitState = decision.nextState
        if (!decision.shouldFinish) return
        dismissPlaybackNotification()
        preparePlayerExitFrame()
        settleOrientationBeforeExit(presentation)
        handler.postDelayed({
            releasePlayerAfterExit()
        }, presentation.releaseDelayMs)
    }

    private fun settleOrientationBeforeExit(presentation: PlayerExitPresentation) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        handler.postDelayed({
            preparePlayerExitFrame()
            finish()
            suppressExitTransition()
        }, presentation.finishDelayMs)
    }

    private fun suppressExitTransition() {
        when {
            PlayerExitPolicy.transitionStrategyFor(Build.VERSION.SDK_INT) ==
                PlayerExitTransitionStrategy.OVERRIDE_ACTIVITY_TRANSITION &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                overrideCloseTransitionCompat()
            }
            else -> {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun overrideCloseTransitionCompat() {
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun overrideOpenTransitionCompat() {
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
    }

    private fun preparePlayerExitFrame() {
        val frame = PlayerExitPolicy.exitFrameDecision(this::playerView.isInitialized)
        if (!frame.shouldPrepare) return
        // Do not detach PlayerView here: Media3 may block while detaching the video surface
        // and throw ExoTimeoutException on some devices during Activity finish.
        if (frame.cancelPlayerViewAnimation) playerView.animate().cancel()
        if (frame.hidePlayerView) playerView.visibility = View.INVISIBLE
        val backdropColorRes = when (frame.backdrop) {
            PlayerExitBackdrop.APP_BASE -> R.color.ov_bg_base
        }
        if (this::playerRoot.isInitialized) {
            playerRoot.setBackgroundColor(ContextCompat.getColor(this, backdropColorRes))
        }
        if (this::firstFrameScrim.isInitialized) {
            firstFrameScrim.animate().cancel()
            firstFrameScrim.setBackgroundColor(ContextCompat.getColor(this, backdropColorRes))
            firstFrameScrim.alpha = 1f
            firstFrameScrim.visibility = View.VISIBLE
            firstFrameScrim.bringToFront()
        }
        if (this::controlsContainer.isInitialized) {
            controlsContainer.animate().cancel()
            controlsContainer.visibility = View.INVISIBLE
        }
        window.setBackgroundDrawableResource(backdropColorRes)
    }

    private fun releasePlayerAfterExit() {
        val decision = PlayerExitPolicy.requestRelease(exitState)
        exitState = decision.nextState
        if (!decision.shouldRelease) return
        dismissPlaybackNotification()
        viewModel.release()
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun formatTime(ms: Long): String = PlayerTimeFormatter.format(ms)

    /**
     * Manifest 使用了 configChanges（方向切换不重建 Activity），
     * 因此这里需要手动重绑布局，才能正确命中 layout-land/player_controls.xml。
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        rebindControlsForConfiguration()
    }

    private fun rebindControlsForConfiguration() {
        if (this::controlsContainer.isInitialized) {
            controlsContainer.removeOnLayoutChangeListener(landscapeGeometryListener)
        }

        enterImmersiveMode()
        setContentView(R.layout.activity_player)
        initViews()
        initGestures()
        initBrightnessAndVolume()

        playerView.player = viewModel.player
        firstFrameScrim.visibility =
            if (PlayerFirstFrameScrimPolicy.scrimVisibleOnReattach(isAwaitingFirstFrame)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        setupControls()
        refreshSessionListButtonVisibility()
        tvTitle.text = viewModel.uiState.value.title
        applyPlayerSettings()
        updateLandResolutionBadge()
        syncPlayPauseIcon()

        controlsContainer.alpha = if (controlsVisible) controlsChromeMaxAlpha() else 0f
        controlsContainer.visibility = if (controlsVisible) View.VISIBLE else View.GONE
        applyControlVisibility()
        if (exitState.isFinishing) {
            preparePlayerExitFrame()
        }
        controlsContainer.post { applyLandscapePlayerGeometry() }
    }

    override fun onResume() {
        super.onResume()
        isActivityForeground = true
        val decision = PlayerLifecyclePolicy.onResume()
        if (decision.stopPlaybackService) stopPlaybackService()
        reattachPlayerSurfaceFromBackground()
        applyDisplaySettings()
        if (decision.observeState) observeState()
    }

    override fun onPause() {
        super.onPause()
        isActivityForeground = false
        stopObservingState()
        val decision = PlayerLifecyclePolicy.onPause(
            isInPictureInPicture = isInPipModeCompat(),
            pauseOnExit = playerPrefs.pauseOnExit,
            backgroundAudio = playerPrefs.bgAudio,
            isPlaying = viewModel.player?.isPlaying == true
        )
        if (decision.saveHistory) viewModel.saveHistory()
        if (decision.pausePlayer) {
            if (decision.unlockBeforePause) unlockPlayerForPause()
            viewModel.player?.pause()
        }
        if (isFinishing) {
            dismissPlaybackNotification()
        } else {
            if (decision.stopPlaybackService) stopPlaybackService()
            if (decision.startPlaybackService) startPlaybackServiceIfNeeded(true)
        }
    }

    override fun onDestroy() {
        dismissPlaybackNotification()
        playbackCoordinator.clearHandlers()
        // unregister prefs listener
        if (this::settingsPrefs.isInitialized) {
            try { settingsPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener) } catch (_: Exception) {}
        }
        handler.removeCallbacksAndMessages(null)
        playerListener?.let { viewModel.player?.removeListener(it) }
        playerListener = null
        startupAnalyticsListener?.let { viewModel.player?.removeAnalyticsListener(it) }
        startupAnalyticsListener = null
        preparePlayerExitFrame()
        releasePlayerAfterExit()
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            applyChromePresentation(PlayerChromeVisibilityPolicy.pictureInPicture())
        } else {
            showControls()
        }
    }

    @OptIn(UnstableApi::class)
    private fun setPlayerResizeMode() {
        playerView.resizeMode = PlayerViewSettings.resizeModeFor(playerPrefs.aspectRatio)
    }

    /**
     * 16:9 / 4:3 等需在 [AspectRatioFrameLayout] 上设置目标宽高比；仅 resizeMode 与 FIT 相同则不会生效。
     * 在 PlayerView 更新视频比例之后调用（本 Activity 的 [Player.Listener] 注册在其后）。
     */
    @OptIn(UnstableApi::class)
    @Suppress("DEPRECATION")
    private fun applyPlayerContentAspectRatio(
        width: Int? = null,
        height: Int? = null,
        pixelWidthHeightRatio: Float? = null,
        unappliedRotationDegrees: Int? = null
    ) {
        if (!this::playerView.isInitialized) return
        val contentFrame = playerView.findViewById<AspectRatioFrameLayout>(Media3UiR.id.exo_content_frame)
            ?: return
        val vs = viewModel.player?.videoSize
        val ratio = PlayerVideoLayoutPolicy.contentAspectRatio(
            preferredAspectRatio = playerPrefs.aspectRatio,
            width = width ?: vs?.width ?: 0,
            height = height ?: vs?.height ?: 0,
            pixelWidthHeightRatio = pixelWidthHeightRatio ?: vs?.pixelWidthHeightRatio ?: 1f,
            unappliedRotationDegrees = unappliedRotationDegrees ?: vs?.unappliedRotationDegrees ?: 0
        )
        contentFrame.setAspectRatio(ratio)
    }

    /**
     * Zooms the active content band (P9-1b) on [exo_content_frame]. Only runs when FIT-style
     * aspect ratio and a non-off [ContentFrameMode] are active; otherwise resets to identity.
     */
    @OptIn(UnstableApi::class)
    @Suppress("DEPRECATION")
    private fun applyPlayerContentFrameTransform(
        width: Int? = null,
        height: Int? = null,
        pixelWidthHeightRatio: Float? = null,
        unappliedRotationDegrees: Int? = null
    ) {
        if (!this::playerView.isInitialized) return
        if (playerView.width <= 0 || playerView.height <= 0) {
            playerView.post {
                applyPlayerContentFrameTransform(
                    width = width,
                    height = height,
                    pixelWidthHeightRatio = pixelWidthHeightRatio,
                    unappliedRotationDegrees = unappliedRotationDegrees
                )
            }
            return
        }
        val contentFrame = playerView.findViewById<AspectRatioFrameLayout>(Media3UiR.id.exo_content_frame)
            ?: return
        val frameSize = contentFrameSourceSize(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
        if (frameSize.width <= 0 || frameSize.height <= 0) {
            resetContentFrameTransform(contentFrame)
            scheduleContentFrameTransformRetry(
                width = width,
                height = height,
                pixelWidthHeightRatio = pixelWidthHeightRatio,
                unappliedRotationDegrees = unappliedRotationDegrees
            )
            return
        }
        contentFrameTransformRetryPosted = false
        cachedBaseContentFrameTransform = PlayerContentFrameApplyPolicy.resolveTransform(
            contentFrameMode = playerPrefs.contentFrameMode,
            aspectRatio = playerPrefs.aspectRatio,
            sourceWidth = frameSize.width,
            sourceHeight = frameSize.height,
            viewportWidth = playerView.width,
            viewportHeight = playerView.height
        )
        if (PlayerVideoZoomPolicy.allowsManualZoom(playerPrefs.aspectRatio)) {
            val (panX, panY) = PlayerVideoZoomPolicy.clampPanOffset(
                panX = manualVideoZoom.panX,
                panY = manualVideoZoom.panY,
                baseScale = cachedBaseContentFrameTransform.scale,
                manualScale = manualVideoZoom.scale,
                frameWidth = contentFrame.width,
                frameHeight = contentFrame.height,
                viewportWidth = playerView.width,
                viewportHeight = playerView.height
            )
            manualVideoZoom = manualVideoZoom.copy(panX = panX, panY = panY)
        }
        val transform = PlayerContentFrameApplyPolicy.resolveTransformWithManualZoom(
            contentFrameMode = playerPrefs.contentFrameMode,
            aspectRatio = playerPrefs.aspectRatio,
            sourceWidth = frameSize.width,
            sourceHeight = frameSize.height,
            viewportWidth = playerView.width,
            viewportHeight = playerView.height,
            manualZoom = manualVideoZoom,
            frameWidth = contentFrame.width,
            frameHeight = contentFrame.height
        )
        applyContentFrameTransform(contentFrame, frameSize, transform)
    }

    private fun contentFrameSourceSize(
        width: Int?,
        height: Int?,
        pixelWidthHeightRatio: Float?,
        unappliedRotationDegrees: Int?
    ): DisplayFrameSize {
        val vs = viewModel.player?.videoSize
        val rawWidth = width?.takeIf { it > 0 }
            ?: vs?.width?.takeIf { it > 0 }
            ?: intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0)
        val rawHeight = height?.takeIf { it > 0 }
            ?: vs?.height?.takeIf { it > 0 }
            ?: intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0)
        return PlayerVideoLayoutPolicy.displayFrameSize(
            width = rawWidth,
            height = rawHeight,
            pixelWidthHeightRatio = pixelWidthHeightRatio ?: vs?.pixelWidthHeightRatio ?: 1f,
            unappliedRotationDegrees = unappliedRotationDegrees ?: vs?.unappliedRotationDegrees ?: 0
        )
    }

    private fun scheduleContentFrameTransformRetry(
        width: Int?,
        height: Int?,
        pixelWidthHeightRatio: Float?,
        unappliedRotationDegrees: Int?
    ) {
        if (contentFrameTransformRetryPosted) return
        contentFrameTransformRetryPosted = true
        playerView.post {
            contentFrameTransformRetryPosted = false
            applyPlayerContentFrameTransform(
                width = width,
                height = height,
                pixelWidthHeightRatio = pixelWidthHeightRatio,
                unappliedRotationDegrees = unappliedRotationDegrees
            )
        }
    }

    private fun resetContentFrameTransform(contentFrame: AspectRatioFrameLayout) {
        contentFrame.pivotX = PlayerContentFrameResetPolicy.PIVOT
        contentFrame.pivotY = PlayerContentFrameResetPolicy.PIVOT
        contentFrame.scaleX = PlayerContentFrameResetPolicy.SCALE
        contentFrame.scaleY = PlayerContentFrameResetPolicy.SCALE
        contentFrame.translationX = PlayerContentFrameResetPolicy.TRANSLATION
        contentFrame.translationY = PlayerContentFrameResetPolicy.TRANSLATION
    }

    private fun applyContentFrameTransform(
        contentFrame: AspectRatioFrameLayout,
        frameSize: DisplayFrameSize,
        transform: PlayerContentFrameTransform
    ) {
        contentFrame.pivotX = PlayerContentFrameResetPolicy.PIVOT
        contentFrame.pivotY = PlayerContentFrameResetPolicy.PIVOT
        contentFrame.scaleX = transform.scale
        contentFrame.scaleY = transform.scale
        contentFrame.translationX = transform.translationX
        contentFrame.translationY = transform.translationY
        if (com.example.openvideo.BuildConfig.DEBUG) {
            android.util.Log.d(
                TAG_CONTENT_FRAME,
                "mode=${playerPrefs.contentFrameMode.key} aspect=${playerPrefs.aspectRatio.key} " +
                    "frame=${frameSize.width}x${frameSize.height} viewport=${playerView.width}x${playerView.height} " +
                    "scale=${transform.scale} tx=${transform.translationX} ty=${transform.translationY}"
            )
        }
    }

    @OptIn(UnstableApi::class)
    private fun videoRenderView(): View? {
        return playerView.videoSurfaceView
    }

    @Suppress("DEPRECATION")
    private fun enterPipModeIfSupported() {
        val videoSize = viewModel.player?.videoSize
        val decision = PlayerPipPolicy.enterDecision(
            sdkInt = Build.VERSION.SDK_INT,
            supportsPictureInPicture = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE),
            isAlreadyInPictureInPicture = isInPipModeCompat(),
            videoWidth = videoSize?.width ?: 0,
            videoHeight = videoSize?.height ?: 0,
            pixelWidthHeightRatio = videoSize?.pixelWidthHeightRatio ?: 1f,
            unappliedRotationDegrees = videoSize?.unappliedRotationDegrees ?: 0
        )
        if (!decision.shouldEnter) return
        runCatching {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(
                        decision.aspectRatio?.toRational() ?: PlayerPipPolicy.fallbackRational()
                    )
                    .build()
            )
        }
    }

    private fun ensureNotificationPermission() {
        val requiresPermission =
            PlayerNotificationPermissionPolicy.requiresRuntimePermission(Build.VERSION.SDK_INT)
        if (!requiresPermission) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val permissionPrefs = getSharedPreferences(PREFS_NOTIFICATION_PERMISSION, Context.MODE_PRIVATE)
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
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun registerPlaybackNotificationHandlers() {
        playbackCoordinator.registerHandlers(
            onTogglePlayPause = {
                togglePlayPauseAndSyncIcon()
                syncPlaybackNotificationSnapshot()
                refreshPlaybackNotificationIfRunning()
            },
            onSkipToNext = { skipQueueVideoFromNotification(forward = true) },
            onSkipToPrevious = { skipQueueVideoFromNotification(forward = false) }
        )
    }

    private fun reattachPlayerSurfaceFromBackground() {
        if (!this::playerView.isInitialized) return
        playerView.visibility = View.VISIBLE
        playerView.player = viewModel.player
        if (!PlayerFirstFrameScrimPolicy.scrimVisibleOnReattach(isAwaitingFirstFrame)) {
            firstFrameScrim.visibility = View.GONE
        }
        syncPlayPauseIcon()
    }

    private fun syncPlaybackNotificationSnapshot() {
        if (!this::tvTitle.isInitialized) return
        val player = viewModel.player
        playbackCoordinator.updateSnapshot(
            PlaybackNotificationCoordinator.Snapshot(
                videoUri = currentVideoUriString,
                title = tvTitle.text.toString(),
                videoId = viewModel.playingVideoId,
                videoPath = currentVideoPath,
                videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0),
                videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0),
                queue = viewModel.sessionQueue.value,
                loopMode = playerPrefs.loopMode,
                isPlaying = player?.isPlaying == true,
                positionMs = player?.currentPosition ?: 0L,
                durationMs = player?.duration?.takeIf { it > 0L } ?: 0L
            )
        )
    }

    private fun refreshPlaybackNotificationIfRunning() {
        if (!PlayerNotificationRefreshPolicy.shouldRefreshBackgroundPlayback(
                isFinishing = isFinishing,
                backgroundAudio = playerPrefs.bgAudio,
                notificationEnabled = playerPrefs.bgPlaybackNotificationEnabled,
                isActivityForeground = isActivityForeground
            )
        ) {
            return
        }
        runCatching {
            ContextCompat.startForegroundService(this, PlaybackServiceIntents.refresh(this))
        }
    }

    private fun dismissPlaybackNotification() {
        playbackCoordinator.clearSnapshot()
        val intent = PlaybackServiceIntents.stop(this)
        runCatching { startService(intent) }
        runCatching { stopService(intent) }
    }

    private fun skipQueueVideoFromNotification(forward: Boolean) {
        val queue = viewModel.sessionQueue.value
        if (!PlayerSessionQueueChromePolicy.canOpenSessionListPanel(queue.size)) return
        val currentIndex = queue.indexOfFirst { it.id == viewModel.playingVideoId }
        val targetIndex = if (forward) {
            PlayerQueueSkipPolicy.nextIndex(currentIndex, queue.size, playerPrefs.loopMode)
        } else {
            PlayerQueueSkipPolicy.previousIndex(currentIndex, queue.size, playerPrefs.loopMode)
        } ?: return
        val item = queue[targetIndex]
        switchSessionVideo(item) {
            currentVideoUriString = item.uri.toString()
            currentVideoPath = item.path
            tvTitle.text = item.title
            loadSubtitlesAsync(
                playerPrefs.externalSubtitleUri.ifBlank { item.uri.toString() },
                item.path
            )
            applyPlayerSettings()
            syncPlaybackNotificationSnapshot()
            refreshPlaybackNotificationIfRunning()
            scheduleHideControls()
        }
    }

    private fun startPlaybackServiceIfNeeded(isPlaying: Boolean) {
        if (isFinishing) return
        val decision = PlayerBackgroundServicePolicy.startDecision(
            backgroundAudio = playerPrefs.bgAudio,
            isPlaying = isPlaying,
            isActivityForeground = isActivityForeground,
            isInPictureInPicture = isInPipModeCompat(),
            notificationEnabled = playerPrefs.bgPlaybackNotificationEnabled
        )
        if (!decision.shouldStart) return
        syncPlaybackNotificationSnapshot()
        val intent = PlaybackServiceIntents.start(
            context = this,
            title = tvTitle.text.toString(),
            isPlaying = isPlaying
        )
        runCatching { ContextCompat.startForegroundService(this, intent) }
    }

    private fun stopPlaybackService() {
        runCatching { stopService(PlaybackServiceIntents.stop(this)) }
    }

    private fun applyPlaybackTickSeek(currentPositionMs: Long, durationMs: Long) {
        val result = PlayerPlaybackTickPolicy.seekTarget(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            abLoopState = abLoopState,
            abLoopPointA = abLoopPointA,
            abLoopPointB = abLoopPointB,
            skipIntroOutro = playerPrefs.skipIntroOutro,
            introSeconds = playerPrefs.introSeconds,
            outroSeconds = playerPrefs.outroSeconds,
            hasSkippedIntro = hasSkippedIntro,
            hasSkippedOutro = hasSkippedOutro,
            clipLoopPreview = playerPrefs.clipLoopPreview,
            clipStartMs = playerPrefs.clipStartMs,
            clipEndMs = playerPrefs.clipEndMs
        ) ?: return

        hasSkippedIntro = result.hasSkippedIntro
        hasSkippedOutro = result.hasSkippedOutro
        viewModel.seekTo(result.positionMs)
    }

    private fun isInPipModeCompat(): Boolean =
        PlayerPipCompatPolicy.isInPictureInPictureMode(
            sdkInt = Build.VERSION.SDK_INT,
            isInPictureInPictureMode = isInPictureInPictureMode
        )

    companion object {
        private const val SUBTITLE_SETTINGS_SHEET_TAG = "player_subtitle_settings_sheet"
        private const val TAG_CONTENT_FRAME = "OVContentFrame"
        private const val PREFS_NOTIFICATION_PERMISSION = "notification_permission"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
        const val EXTRA_VIDEO_WIDTH = "video_width"
        const val EXTRA_VIDEO_HEIGHT = "video_height"
        const val EXTRA_START_POSITION_MS = "start_position_ms"
        const val EXTRA_FROM_PLAYBACK_NOTIFICATION = "from_playback_notification"
    }
}
