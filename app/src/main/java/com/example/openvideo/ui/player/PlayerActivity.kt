package com.example.openvideo.ui.player

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
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.FrameLayout
import android.widget.ImageView
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
import androidx.media3.ui.PlayerView
import com.example.openvideo.R
import com.example.openvideo.core.diagnostics.CrashLogger
import com.example.openvideo.core.media.LocalMediaUriPolicy
import com.example.openvideo.core.player.PlaybackNotificationCoordinator
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SubtitleBgStyle
import com.example.openvideo.core.subtitle.SubtitleLoader
import com.example.openvideo.data.model.VideoItem
import dagger.hilt.android.AndroidEntryPoint
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

    private var seekThumbnailLoader: PlayerSeekThumbnailLoader? = null
    private lateinit var seekThumbnailContainer: FrameLayout
    private lateinit var seekThumbnailImage: ImageView
    private lateinit var seekThumbnailTime: TextView
    private lateinit var progressRow: View

    /** 横屏布局专有：底部工具栏「列表」入口。 */
    private var btnVideoList: View? = null

    /** 横屏右侧浮层（倍速 / 比例 / 画中画）。 */
    private var landRightFloatColumn: View? = null

    private var manualVideoZoom = PlayerVideoZoomState.IDENTITY

    // Error HUD views (initialized in initViews)
    private lateinit var playerErrorHud: View
    private lateinit var tvErrorTitle: TextView
    private lateinit var tvErrorDesc: TextView
    private var btnErrorSoftDecode: Button? = null
    private var btnErrorRetry: Button? = null
    private var btnErrorCopyDiag: Button? = null
    private var btnErrorBack: Button? = null

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
    private var controlsVisibleBeforeSettingsOverlay = true
    private val hideControlsRunnable = Runnable { hideControls() }
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

    private var currentBrightness = 0.5f
    private var currentVolume = 0.5f
    private var isSeeking = false

    // AB Loop state
    private var abLoopState = PlayerAbLoopState.IDLE
    private var abLoopPointA: Long = -1
    private var abLoopPointB: Long = -1

    // Screen lock state
    private var isScreenLocked = false
    private var playerListener: Player.Listener? = null
    private var startupAnalyticsListener: androidx.media3.exoplayer.analytics.AnalyticsListener? = null

    // 一旦用户手动切过屏幕方向（点击全屏按钮等），本次视频会话内禁止自动方向覆盖，
    // 防止 onVideoSizeChanged 等回调把用户操作冲掉。切到下一首视频时复位。
    private var userOverrodeOrientation = false
    private var playbackWasBuffering = false
    private var hasSkippedIntro = false
    private var hasSkippedOutro = false
    private val startupTrace = PlayerStartupTrace()
    private var lastHistorySavedPositionMs = 0L

    /** 单次手势起始亮度/音量（0–1），避免 MOVE 期间重复累加误差 */

    private lateinit var pickSubtitleLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private val videoOrientation by lazy {
        PlayerVideoOrientationController(
            activity = this,
            playerPrefs = playerPrefs,
            userOverrodeOrientationProvider = { userOverrodeOrientation },
            onUserOverrodeOrientationChanged = { userOverrodeOrientation = it }
        )
    }
    private val quickDialogs by lazy {
        PlayerQuickDialogController(
            activity = this,
            playerManager = playerManager,
            viewModel = viewModel,
            playerPrefs = playerPrefs,
            pickSubtitleLauncher = pickSubtitleLauncher,
            handler = handler,
            hideControlsRunnable = hideControlsRunnable,
            controlsVisibleProvider = { controlsVisible },
            onControlsVisibleBeforeSettingsOverlayChanged = { controlsVisibleBeforeSettingsOverlay = it },
            isSettingsOverlayVisibleProvider = { isSettingsOverlayVisible },
            onSettingsOverlayVisibleChanged = { isSettingsOverlayVisible = it },
            onHideControls = ::hideControls,
            onHideChromeForSettingsOverlay = ::hideChromeForSettingsOverlay,
            onRestoreChromeAfterSettingsOverlay = ::restoreChromeAfterSettingsOverlay,
            onScheduleHideControls = ::scheduleHideControls,
            onApplyScreenBrightness = ::applyScreenBrightness,
            onAspectRatioChanged = ::applyAspectRatioDisplayChange,
            onApplyPlayerSettings = ::applyPlayerSettings,
            onApplySubtitlePresentation = ::applySubtitlePresentation,
            onSessionVideoPicked = { item ->
                switchSessionVideo(item) {
                    currentVideoUriString = item.uri.toString()
                    currentVideoPath = item.path
                    tvTitle.text = item.title
                    loadSubtitlesAsync(playerPrefs.externalSubtitleUri.ifBlank { item.uri.toString() }, item.path)
                    applyPlayerSettings()
                    scheduleHideControls()
                }
            }
        )
    }
    private val smartCrop: PlayerSmartCropController by lazy {
        PlayerSmartCropController(
            activity = this,
            playerPrefs = playerPrefs,
            viewModel = viewModel,
            handler = handler,
            hideControlsRunnable = hideControlsRunnable,
            playerViewProvider = { playerView },
            controlsContainerProvider = { controlsContainer },
            contentFrameSourceSizeProvider = contentFrameTransforms::sourceSize,
            videoRenderViewProvider = contentFrameTransforms::videoRenderView,
            onControlsVisibleChanged = { controlsVisible = it },
            onApplyControlVisibility = ::applyControlVisibility,
            onResetManualVideoZoom = { manualVideoZoom = PlayerVideoZoomState.IDENTITY },
            onApplyDisplaySettings = ::applyDisplaySettings,
            onScheduleHideControls = ::scheduleHideControls
        )
    }
    private val contentFrameTransforms: PlayerContentFrameTransformController by lazy {
        PlayerContentFrameTransformController(
            playerPrefs = playerPrefs,
            viewModel = viewModel,
            playerViewProvider = { playerView },
            intentProvider = { intent },
            manualVideoZoomProvider = { manualVideoZoom },
            onManualVideoZoomChanged = { manualVideoZoom = it },
            smartCropStateProvider = {
                PlayerSmartCropTransformState(
                    contentFrameMode = smartCrop.contentFrameMode,
                    transformOverride = smartCrop.transformOverride,
                    viewportFillFraction = smartCrop.viewportFillFraction,
                    viewportScale = smartCrop.viewportScale,
                    cropExpansionFraction = smartCrop.cropExpansionFraction
                )
            }
        )
    }
    private val playerGestures: PlayerGestureController by lazy {
        PlayerGestureController(
            activity = this,
            playerPrefs = playerPrefs,
            viewModel = viewModel,
            handler = handler,
            gestureOverlayProvider = { gestureOverlay },
            seekIndicatorProvider = { seekIndicator },
            brightnessIndicatorProvider = { brightnessIndicator },
            brightnessProgressProvider = { brightnessProgress },
            volumeIndicatorProvider = { volumeIndicator },
            volumeProgressProvider = { volumeProgress },
            currentBrightnessProvider = { currentBrightness },
            onCurrentBrightnessChanged = { currentBrightness = it },
            currentVolumeProvider = { currentVolume },
            onCurrentVolumeChanged = { currentVolume = it },
            manualVideoZoomProvider = { manualVideoZoom },
            onManualVideoZoomChanged = { manualVideoZoom = it },
            baseContentFrameScaleProvider = { contentFrameTransforms.baseTransform.scale },
            playerViewportWidthProvider = { playerView.width },
            playerViewportHeightProvider = { playerView.height },
            isScreenLockedProvider = { isScreenLocked },
            controlsVisibleProvider = { controlsVisible },
            onShowControls = ::showControls,
            onHideControls = ::hideControls,
            onShowLockedControls = ::showLockedControls,
            onApplyContentFrameTransform = ::applyPlayerContentFrameTransform,
            onSetWindowBrightness = ::setWindowBrightness,
            onFinishPlayer = ::finishPlayer
        )
    }
    private val firstFrames: PlayerFirstFrameController by lazy {
        PlayerFirstFrameController(
            activity = this,
            handler = handler,
            startupTrace = startupTrace,
            firstFrameScrimProvider = { if (this::firstFrameScrim.isInitialized) firstFrameScrim else null },
            hasVideoTrackProvider = ::hasVideoTrack
        )
    }
    private val errorHud: PlayerErrorHudController by lazy {
        PlayerErrorHudController(
            activity = this,
            viewModel = viewModel,
            playerErrorHudProvider = { playerErrorHud },
            titleProvider = { tvErrorTitle },
            descProvider = { tvErrorDesc },
            softDecodeButtonProvider = { btnErrorSoftDecode },
            retryButtonProvider = { btnErrorRetry },
            copyDiagnosticsButtonProvider = { btnErrorCopyDiag },
            backButtonProvider = { btnErrorBack },
            controlsContainerProvider = { controlsContainer },
            firstFrameScrimProvider = { firstFrameScrim },
            onShowControls = ::showControls,
            onFinishPlayer = ::finishPlayer
        )
    }
    private val playbackNotifications by lazy {
        PlayerPlaybackNotificationController(
            activity = this,
            playerPrefs = playerPrefs,
            viewModel = viewModel,
            playbackCoordinator = playbackCoordinator,
            notificationPermissionLauncher = notificationPermissionLauncher,
            playerViewProvider = { if (this::playerView.isInitialized) playerView else null },
            firstFrameScrimProvider = { if (this::firstFrameScrim.isInitialized) firstFrameScrim else null },
            titleProvider = { if (this::tvTitle.isInitialized) tvTitle.text.toString() else null },
            currentVideoUriStringProvider = { currentVideoUriString },
            currentVideoPathProvider = { currentVideoPath },
            intentProvider = { intent },
            isActivityForegroundProvider = { isActivityForeground },
            isAwaitingFirstFrameProvider = { firstFrames.isAwaitingFirstFrame },
            isInPipModeProvider = ::isInPipModeCompat,
            onTogglePlayPause = ::togglePlayPauseAndSyncIcon,
            onSyncPlayPauseIcon = ::syncPlayPauseIcon,
            onSwitchSessionVideo = ::switchSessionVideo,
            onCurrentVideoChanged = { item ->
                currentVideoUriString = item.uri.toString()
                currentVideoPath = item.path
                tvTitle.text = item.title
            },
            onLoadSubtitles = { uriString, videoPath ->
                loadSubtitlesAsync(uriString, videoPath)
            },
            onApplyPlayerSettings = ::applyPlayerSettings,
            onScheduleHideControls = ::scheduleHideControls
        )
    }

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
            if (PlayerFirstFrameScrimPolicy.initialScrimVisible(firstFrames.isAwaitingFirstFrame, warmResume)) {
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

        playerErrorHud = findViewById(R.id.player_error_hud)
        tvErrorTitle = findViewById(R.id.tv_error_title)
        tvErrorDesc = findViewById(R.id.tv_error_desc)
        btnErrorSoftDecode = findViewById(R.id.btn_error_soft_decode)
        btnErrorRetry = findViewById(R.id.btn_error_retry)
        btnErrorCopyDiag = findViewById(R.id.btn_error_copy_diag)
        btnErrorBack = findViewById(R.id.btn_error_back)

        seekThumbnailContainer = findViewById(R.id.seek_thumbnail_container)
        seekThumbnailImage = findViewById(R.id.seek_thumbnail_image)
        seekThumbnailTime = findViewById(R.id.seek_thumbnail_time)
        progressRow = findViewById(R.id.progress_row)
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

    private fun showSessionVideoListPanel() = quickDialogs.showSessionVideoListPanel()

    /** 与横屏齿轮按钮相同：弹出播放器设置。竖屏底栏「更多」亦指向此处。 */
    private fun openPlayerSettingsDialog() = quickDialogs.openPlayerSettingsDialog()

    private fun applyAspectRatioDisplayChange() {
        clearSmartCropSession()
        applyDisplaySettings()
    }

    private fun showAspectRatioQuickDialog() = quickDialogs.showAspectRatioQuickDialog()

    private fun handleSmartCropQuickToggle() = smartCrop.handleQuickToggle()

    private fun clearSmartCropSession() = smartCrop.clearSession()

    private fun showSpeedPickerDialog() = quickDialogs.showSpeedPickerDialog()

    private fun showAudioTrackQuickDialog() = quickDialogs.showAudioTrackQuickDialog()

    private fun showSubtitleQuickDialog() = quickDialogs.showSubtitleQuickDialog()

    private fun openSubtitleSettingsSheet() = quickDialogs.openSubtitleSettingsSheet()

    private fun onSubtitleSettingsSheetDismissed() = quickDialogs.onSubtitleSettingsSheetDismissed()

    private fun dismissSubtitleSettingsSheet() = quickDialogs.dismissSubtitleSettingsSheet()

    private fun switchSessionVideo(item: VideoItem, onSwitched: () -> Unit = {}) {
        dismissSubtitleSettingsSheet()
        showFirstFrameScrim()
        preApplyOrientationForItem(item)
        viewModel.switchToVideo(item) {
            resetPlaybackSessionForNewVideo()
            onSwitched()
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

        findViewById<View>(R.id.btn_land_smart_crop)?.setPlayerClickListener(PlayerLockedInteraction.SETTINGS) {
            handleSmartCropQuickToggle()
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

                    // Seek thumbnail integration
                    val loader = seekThumbnailLoader
                    if (loader != null) {
                        val currentVideoUri = viewModel.currentVideoUri
                        if (currentVideoUri != null) {
                            val duration = viewModel.uiState.value.duration
                            val positionMs = PlayerSeekThumbnailPolicy.thumbnailPositionMs(progress, sb.max, duration)

                            // Update time text
                            seekThumbnailTime.text = formatTime(positionMs)

                            // Load thumbnail
                            loader.loadThumbnail(currentVideoUri, positionMs) { bitmap ->
                                if (bitmap != null) {
                                    seekThumbnailImage.setImageBitmap(bitmap)
                                }
                            }

                            // Dynamic translationX alignment above the seek thumb
                            val usableWidth = sb.width - sb.paddingLeft - sb.paddingRight
                            val pct = if (sb.max > 0) progress.toFloat() / sb.max else 0f
                            val absoluteSeekBarLeft = bottomPanel.left + progressRow.left + sb.left
                            val thumbXInParent = absoluteSeekBarLeft + sb.paddingLeft + (usableWidth * pct) - (seekThumbnailContainer.width / 2f)

                            seekThumbnailContainer.translationX = thumbXInParent.coerceIn(
                                0f,
                                (controlsContainer.width - seekThumbnailContainer.width).toFloat()
                            )
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar) {
                if (!PlayerLockedControlsPolicy.allows(PlayerLockedInteraction.SEEK_BAR, isScreenLocked)) return
                isSeeking = true
                handler.removeCallbacks(hideControlsRunnable)

                // Seek thumbnail integration
                if (playerPrefs.seekThumbnailEnabled) {
                    val currentVideoUri = viewModel.currentVideoUri
                    val scheme = currentVideoUri?.scheme
                    if (currentVideoUri != null && !PlayerSeekThumbnailPolicy.shouldSkipThumbnail(scheme)) {
                        seekThumbnailLoader = PlayerSeekThumbnailLoader(this@PlayerActivity)
                        seekThumbnailContainer.visibility = View.VISIBLE
                    }
                }
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

                // Seek thumbnail integration
                seekThumbnailContainer.visibility = View.GONE
                seekThumbnailLoader?.release()
                seekThumbnailLoader = null
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
                    firstFrames.hideForAudioOnly()
                    errorHud.hide()
                    firstFrames.onPrepareReady()
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
                firstFrames.onRenderedFirstFrame()
            }

            override fun onPlayerError(error: PlaybackException) {
                CrashLogger.logPlayerError(this@PlayerActivity, error)
                errorHud.show(error)
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
        firstFrames.showForNewMedia()
    }

    private fun hideFirstFrameScrim() {
        firstFrames.hideOnRenderedFirstFrame()
    }

    /**
     * 第一次 `STATE_READY` 到达后打 `prepare_ready` 埋点并排程「首帧迟滞」检查。
     */
    private fun onPrepareReady() {
        firstFrames.onPrepareReady()
    }

    private fun scheduleFirstFrameTimeoutCheck() {
        firstFrames.onPrepareReady()
    }

    private fun cancelFirstFrameTimeoutCheck() {
        firstFrames.cancelTimeoutCheck()
    }

    private val firstFrameTimeoutRunnable = Runnable {
        firstFrames.onPrepareReady()
    }

    private fun applyFirstFrameDecision(decision: PlayerFirstFrameDecision) {
        if (decision.nextAwaitingFirstFrame) firstFrames.showForNewMedia() else firstFrames.hideOnRenderedFirstFrame()
    }

    /**
     * 展示播放错误 HUD。隐藏控制层和 scrim，绑定 [PlayerErrorPresentationPolicy] 的展示模型。
     */
    private fun showPlayerErrorHud(error: PlaybackException) {
        errorHud.show(error)
    }

    /** 播放恢复正常时隐藏错误 HUD，恢复控制层。 */
    private fun hidePlayerErrorHud() {
        errorHud.hide()
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
        firstFrames.resetForNewVideo(reset.awaitFirstFrame)
        manualVideoZoom = reset.manualVideoZoom
        playbackWasBuffering = false
        lastHistorySavedPositionMs = PlaybackProgressPolicy.onNewMedia()
        btnAbLoop?.clearColorFilter()
        playerGestures.resetForNewVideo(reset)
    }

    private fun initGestures() = playerGestures.init()

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
    ) = videoOrientation.apply(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )

    private fun applyInitialVideoOrientation() = videoOrientation.applyInitial()

    /**
     * 切换视频前，用 MediaStore 已记录的宽高预设方向，避免「先横屏→再竖屏」过渡。
     * ExoPlayer 解码完成后 `onVideoSizeChanged` 会用精确尺寸再次校正。
     * 新视频开始播放时复位用户手动方向标志，让自动方向重新生效。
     */
    private fun preApplyOrientationForItem(item: VideoItem) = videoOrientation.preApplyForItem(item)

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
            if (PlayerFirstFrameScrimPolicy.scrimVisibleOnReattach(firstFrames.isAwaitingFirstFrame)) {
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
    private fun applyPlayerContentAspectRatio(
        width: Int? = null,
        height: Int? = null,
        pixelWidthHeightRatio: Float? = null,
        unappliedRotationDegrees: Int? = null
    ) {
        if (!this::playerView.isInitialized) return
        contentFrameTransforms.applyAspectRatio(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
    }

    private fun applyPlayerContentFrameTransform(
        width: Int? = null,
        height: Int? = null,
        pixelWidthHeightRatio: Float? = null,
        unappliedRotationDegrees: Int? = null
    ) {
        if (!this::playerView.isInitialized) return
        contentFrameTransforms.applyTransform(
            width = width,
            height = height,
            pixelWidthHeightRatio = pixelWidthHeightRatio,
            unappliedRotationDegrees = unappliedRotationDegrees
        )
    }

    private fun contentFrameSourceSize(
        width: Int?,
        height: Int?,
        pixelWidthHeightRatio: Float?,
        unappliedRotationDegrees: Int?
    ): DisplayFrameSize = contentFrameTransforms.sourceSize(
        width = width,
        height = height,
        pixelWidthHeightRatio = pixelWidthHeightRatio,
        unappliedRotationDegrees = unappliedRotationDegrees
    )

    @OptIn(UnstableApi::class)
    private fun videoRenderView(): View? = contentFrameTransforms.videoRenderView()

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
        playbackNotifications.ensurePermission()
    }

    private fun registerPlaybackNotificationHandlers() {
        playbackNotifications.registerHandlers()
    }

    private fun reattachPlayerSurfaceFromBackground() {
        playbackNotifications.reattachPlayerSurfaceFromBackground()
    }

    private fun syncPlaybackNotificationSnapshot() {
        playbackNotifications.syncSnapshot()
    }

    private fun refreshPlaybackNotificationIfRunning() {
        playbackNotifications.refreshIfRunning()
    }

    private fun dismissPlaybackNotification() {
        playbackNotifications.dismiss()
    }

    private fun skipQueueVideoFromNotification(forward: Boolean) {
        playbackNotifications.skipQueueVideo(forward)
    }

    private fun startPlaybackServiceIfNeeded(isPlaying: Boolean) {
        playbackNotifications.startIfNeeded(isPlaying)
    }

    private fun stopPlaybackService() {
        playbackNotifications.stop()
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
        private const val TAG_CONTENT_FRAME = "OVContentFrame"
        const val EXTRA_VIDEO_WIDTH = "video_width"
        const val EXTRA_VIDEO_HEIGHT = "video_height"
        const val EXTRA_START_POSITION_MS = "start_position_ms"
        const val EXTRA_FROM_PLAYBACK_NOTIFICATION = "from_playback_notification"
    }
}
