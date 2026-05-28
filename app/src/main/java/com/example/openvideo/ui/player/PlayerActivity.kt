package com.example.openvideo.ui.player

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.openvideo.R
import com.example.openvideo.core.media.LocalMediaUriPolicy
import com.example.openvideo.core.player.PlaybackNotificationCoordinator
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.PlayerPrefs
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

    private var currentBrightness = 0.5f
    private var currentVolume = 0.5f
    private var isSeeking = false

    // Screen lock state
    private var isScreenLocked = false

    // 一旦用户手动切过屏幕方向（点击全屏按钮等），本次视频会话内禁止自动方向覆盖，
    // 防止 onVideoSizeChanged 等回调把用户操作冲掉。切到下一首视频时复位。
    private var userOverrodeOrientation = false
    private var playbackWasBuffering = false
    private val startupTrace = PlayerStartupTrace()

    /** 单次手势起始亮度/音量（0–1），避免 MOVE 期间重复累加误差 */

    private lateinit var pickSubtitleLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private val subtitles: PlayerSubtitleController by lazy {
        PlayerSubtitleController(
            activity = this,
            viewModel = viewModel,
            playerPrefs = playerPrefs,
            subtitleLoader = subtitleLoader,
            startupTrace = startupTrace,
            onApplyScreenBrightness = ::applyScreenBrightness,
            onApplyPlayerSettings = ::applyPlayerSettings,
            onScheduleHideControls = ::scheduleHideControls
        )
    }
    private val playerChrome: PlayerChromeController by lazy {
        PlayerChromeController(
            activity = this,
            handler = handler,
            hideControlsRunnable = hideControlsRunnable,
            controlsContainerProvider = { controlsContainer },
            topScrimProvider = { topScrim },
            bottomScrimProvider = { bottomScrim },
            topBarProvider = { topBar },
            bottomPanelProvider = { bottomPanel },
            toolRowProvider = { toolRow },
            landRightFloatColumnProvider = { landRightFloatColumn },
            lockButtonProvider = { btnLock },
            fullscreenButtonProvider = { btnFullscreen },
            isScreenLockedProvider = { isScreenLocked },
            onScreenLockedChanged = { isScreenLocked = it },
            controlsVisibleProvider = { controlsVisible },
            onControlsVisibleChanged = { controlsVisible = it },
            isSettingsOverlayVisibleProvider = { isSettingsOverlayVisible },
            controlsVisibleBeforeSettingsOverlayProvider = { controlsVisibleBeforeSettingsOverlay },
            onSetLockedGestureOverlay = ::setLockedGestureOverlay,
            onInitGestures = ::initGestures,
            controlsOpacityPercentProvider = { playerPrefs.controlsOpacity },
            controlsAutoHideSecondsProvider = { playerPrefs.controlsAutoHide },
            controlsChromeMaxAlpha = ::controlsChromeMaxAlpha
        )
    }
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
            onAspectRatioChanged = {
                smartCrop.clearSession()
                applyDisplaySettings()
            },
            onApplyPlayerSettings = ::applyPlayerSettings,
            onApplySubtitlePresentation = { playbackTicks.applySubtitlePresentation() },
            onSessionVideoPicked = { item ->
                switchSessionVideo(item) {
                    subtitles.setCurrentVideo(item.uri.toString(), item.path)
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
    private val playerDisplay: PlayerDisplayController by lazy {
        PlayerDisplayController(
            activity = this,
            window = window,
            playerManager = playerManager,
            viewModel = viewModel,
            playerPrefs = playerPrefs,
            playerViewProvider = { playerView },
            bottomPanelProvider = { bottomPanel },
            controlsContainerProvider = { controlsContainer },
            subtitleProvider = { tvSubtitle },
            brightnessProgressProvider = { brightnessProgress },
            volumeProgressProvider = { volumeProgress },
            topBarProvider = { topBar },
            lockButtonProvider = { btnLock },
            landRightFloatColumnProvider = { landRightFloatColumn },
            controlsVisibleProvider = { controlsVisible },
            onCurrentBrightnessChanged = { currentBrightness = it },
            onCurrentVolumeChanged = { currentVolume = it },
            onResetManualVideoZoom = { manualVideoZoom = PlayerVideoZoomState.IDENTITY },
            controlsChromeMaxAlpha = ::controlsChromeMaxAlpha,
            onApplyContentAspectRatio = ::applyPlayerContentAspectRatio,
            onApplyContentFrameTransform = ::applyPlayerContentFrameTransform,
            onSetWindowBrightness = ::setWindowBrightness
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
    private val abLoop: PlayerAbLoopController by lazy {
        PlayerAbLoopController(
            activity = this,
            buttonProvider = { btnAbLoop },
            formatTime = ::formatTime
        )
    }
    private val playbackTicks: PlayerPlaybackTickController by lazy {
        PlayerPlaybackTickController(
            activity = this,
            handler = handler,
            viewModel = viewModel,
            playerPrefs = playerPrefs,
            seekBarProvider = { seekBar },
            currentTimeProvider = { tvCurrentTime },
            totalTimeProvider = { tvTotalTime },
            subtitleProvider = { tvSubtitle },
            isSeekingProvider = { isSeeking },
            abLoopStateProvider = { abLoop.state },
            abLoopPointAProvider = { abLoop.pointA },
            abLoopPointBProvider = { abLoop.pointB },
            formatTime = ::formatTime,
            landSpeedLabel = ::landSpeedLabel
        )
    }
    private val playbackEnd: PlayerPlaybackEndController by lazy {
        PlayerPlaybackEndController(
            viewModel = viewModel,
            playerPrefs = playerPrefs,
            abLoopStateProvider = { abLoop.state },
            abLoopPointAProvider = { abLoop.pointA },
            onSwitchSessionVideo = ::switchSessionVideo,
            onCurrentVideoChanged = { item ->
                subtitles.setCurrentVideo(item.uri.toString(), item.path)
                tvTitle.text = item.title
            },
            onLoadSubtitles = ::loadSubtitlesAsync,
            onApplyPlayerSettings = ::applyPlayerSettings,
            onScheduleHideControls = ::scheduleHideControls,
            onShowControls = ::showControls,
            onFinishPlayer = ::finishPlayer
        )
    }
    private val seekBars: PlayerSeekBarController by lazy {
        PlayerSeekBarController(
            activity = this,
            viewModel = viewModel,
            playerPrefs = playerPrefs,
            seekBarProvider = { seekBar },
            currentTimeProvider = { tvCurrentTime },
            thumbnailContainerProvider = { seekThumbnailContainer },
            thumbnailImageProvider = { seekThumbnailImage },
            thumbnailTimeProvider = { seekThumbnailTime },
            bottomPanelProvider = { bottomPanel },
            progressRowProvider = { progressRow },
            controlsContainerProvider = { controlsContainer },
            isScreenLockedProvider = { isScreenLocked },
            onSeekingChanged = { isSeeking = it },
            onRemoveHideControlsCallbacks = { handler.removeCallbacks(hideControlsRunnable) },
            onScheduleHideControls = ::scheduleHideControls,
            formatTime = ::formatTime
        )
    }
    private val controlsBinder: PlayerControlsBinder by lazy {
        PlayerControlsBinder(
            activity = this,
            viewModel = viewModel,
            playerManager = playerManager,
            playButtonProvider = { btnPlay },
            backButtonProvider = { btnBack },
            prevButtonProvider = { btnPrev },
            nextButtonProvider = { btnNext },
            videoListButtonProvider = { btnVideoList },
            screenshotButtonProvider = { btnScreenshot },
            abLoopButtonProvider = { btnAbLoop },
            fullscreenButtonProvider = { btnFullscreen },
            pipButtonProvider = { btnPip },
            lockButtonProvider = { btnLock },
            controlsContainerProvider = { controlsContainer },
            isScreenLockedProvider = { isScreenLocked },
            controlsVisibleProvider = { controlsVisible },
            onTogglePlayPause = ::togglePlayPauseAndSyncIcon,
            onFinishPlayer = ::finishPlayer,
            onShowSessionVideoListPanel = { quickDialogs.showSessionVideoListPanel() },
            onOpenPlayerSettingsDialog = { quickDialogs.openPlayerSettingsDialog() },
            onShowSpeedPickerDialog = { quickDialogs.showSpeedPickerDialog() },
            onHandleSmartCropQuickToggle = { smartCrop.handleQuickToggle() },
            onShowAspectRatioQuickDialog = { quickDialogs.showAspectRatioQuickDialog() },
            onShowSubtitleQuickDialog = { quickDialogs.showSubtitleQuickDialog() },
            onScheduleHideControls = ::scheduleHideControls,
            onEnterPipModeIfSupported = ::enterPipModeIfSupported,
            onShowAudioTrackQuickDialog = { quickDialogs.showAudioTrackQuickDialog() },
            onToggleAbLoop = { positionMs -> abLoop.toggle(positionMs) },
            onUserOverrodeOrientationChanged = { userOverrodeOrientation = it },
            onRequestedOrientationChanged = { requestedOrientation = it },
            currentOrientationProvider = { resources.configuration.orientation },
            videoRenderViewProvider = { videoRenderView() },
            onToggleScreenLock = ::toggleScreenLock,
            onHideControls = ::hideControls,
            onShowControls = ::showControls
        )
    }
    private val playerEvents: PlayerEventController by lazy {
        PlayerEventController(
            activity = this,
            viewModel = viewModel,
            playerPrefs = playerPrefs,
            startupTrace = startupTrace,
            playbackWasBufferingProvider = { playbackWasBuffering },
            onPlaybackWasBufferingChanged = { playbackWasBuffering = it },
            onUpdatePlayPauseIcon = ::updatePlayPauseIcon,
            onStartPlaybackServiceIfNeeded = { isPlaying ->
                playbackNotifications.startIfNeeded(isPlaying)
            },
            onUnlockPlayerForPause = ::unlockPlayerForPause,
            onApplyPlaybackTickSeek = { positionMs, durationMs ->
                playbackTicks.applyPlaybackTickSeek(positionMs, durationMs)
            },
            onHideFirstFrameForAudioOnly = { firstFrames.hideForAudioOnly() },
            onHideErrorHud = { errorHud.hide() },
            onPrepareReady = { firstFrames.onPrepareReady() },
            onPlaybackEnded = { playbackEnd.handleEnded() },
            onSetVolumeBoost = { enabled -> viewModel.setVolumeBoost(enabled) },
            onFirstFrameRendered = { firstFrames.onRenderedFirstFrame() },
            onShowPlayerError = { error -> errorHud.show(error) },
            onApplyVideoOrientation = ::applyVideoOrientation,
            onApplyContentAspectRatio = ::applyPlayerContentAspectRatio,
            onApplyContentFrameTransform = ::applyPlayerContentFrameTransform
        )
    }
    private val playerExit: PlayerExitController by lazy {
        PlayerExitController(
            activity = this,
            handler = handler,
            viewModel = viewModel,
            playerViewProvider = { if (this::playerView.isInitialized) playerView else null },
            playerRootProvider = { if (this::playerRoot.isInitialized) playerRoot else null },
            firstFrameScrimProvider = { if (this::firstFrameScrim.isInitialized) firstFrameScrim else null },
            controlsContainerProvider = { if (this::controlsContainer.isInitialized) controlsContainer else null },
            onDismissPlaybackNotification = { playbackNotifications.dismiss() }
        )
    }
    private val playerPip: PlayerPipController by lazy {
        PlayerPipController(
            activity = this,
            viewModel = viewModel,
            onApplyPipChrome = {
                applyChromePresentation(PlayerChromeVisibilityPolicy.pictureInPicture())
            },
            onShowControls = ::showControls
        )
    }
    private val playerLifecycle: PlayerLifecycleController by lazy {
        PlayerLifecycleController(
            viewModel = viewModel,
            playerPrefs = playerPrefs,
            isInPictureInPictureProvider = ::isInPipModeCompat,
            isFinishingProvider = { isFinishing },
            onStopPlaybackService = { playbackNotifications.stop() },
            onReattachPlayerSurface = { playbackNotifications.reattachPlayerSurfaceFromBackground() },
            onApplyDisplaySettings = ::applyDisplaySettings,
            onObserveState = ::observeState,
            onStopObservingState = ::stopObservingState,
            onUnlockPlayerForPause = ::unlockPlayerForPause,
            onDismissPlaybackNotification = { playbackNotifications.dismiss() },
            onStartPlaybackServiceIfNeeded = { isPlaying ->
                playbackNotifications.startIfNeeded(isPlaying)
            },
            onActivityForegroundChanged = { isActivityForeground = it }
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
            currentVideoUriStringProvider = { subtitles.currentVideoUriString },
            currentVideoPathProvider = { subtitles.currentVideoPath },
            intentProvider = { intent },
            isActivityForegroundProvider = { isActivityForeground },
            isAwaitingFirstFrameProvider = { firstFrames.isAwaitingFirstFrame },
            isInPipModeProvider = ::isInPipModeCompat,
            onTogglePlayPause = ::togglePlayPauseAndSyncIcon,
            onSyncPlayPauseIcon = ::syncPlayPauseIcon,
            onSwitchSessionVideo = ::switchSessionVideo,
            onCurrentVideoChanged = { item ->
                subtitles.setCurrentVideo(item.uri.toString(), item.path)
                tvTitle.text = item.title
            },
            onLoadSubtitles = { uriString, videoPath ->
                loadSubtitlesAsync(uriString, videoPath)
            },
            onApplyPlayerSettings = ::applyPlayerSettings,
            onScheduleHideControls = ::scheduleHideControls
        )
    }

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
        PlayerSystemUiController.suppressNotificationOpenTransition(this, intent, EXTRA_FROM_PLAYBACK_NOTIFICATION)
        applyInitialVideoOrientation()
        pickSubtitleLauncher = subtitles.registerPickSubtitleLauncher()
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* 用户拒绝也无需处理：通知不显示，但播放仍可继续 */ }
        playbackNotifications.ensurePermission()
        PlayerSystemUiController.enterImmersiveMode(this)
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
        subtitles.setCurrentVideo(uriString, videoPath)
        val explicitStartPositionMs = intent.getLongExtra(EXTRA_START_POSITION_MS, 0L)
        if (warmResume) {
            applyPlayerSettings()
            syncPlayPauseIcon()
            playbackNotifications.syncSnapshot()
        } else {
            viewModel.restorePlaybackPreferences(id) {
                applyPlayerSettings()
                loadSubtitlesAsync(playerPrefs.externalSubtitleUri.ifBlank { uriString }, videoPath)
            }
        }

        subtitles.registerPrefsListener()

        scheduleHideControls()

        // Restore playback position if remember_progress is on
        if (PlayerSessionResumePolicy.shouldRestorePlaybackPosition(playerPrefs.rememberProgress)) {
            viewModel.restorePosition(id, explicitStartPositionMs)
        }

        controlsContainer.post { applyLandscapePlayerGeometry() }
        playbackNotifications.registerHandlers()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PlayerSystemUiController.suppressNotificationOpenTransition(this, intent, EXTRA_FROM_PLAYBACK_NOTIFICATION)
        playbackNotifications.reattachPlayerSurfaceFromBackground()
    }

    private fun applyPlayerSettings() = playerDisplay.applyPlayerSettings()

    internal fun refreshPlayerDisplayFromSettings() {
        applyDisplaySettings()
    }

    private fun applyDisplaySettings() = playerDisplay.applyDisplaySettings()

    private fun initBrightnessAndVolume() = playerDisplay.initBrightnessAndVolume()

    private fun loadSubtitlesAsync(uriString: String, videoPath: String, showToast: Boolean = false) {
        subtitles.loadSubtitlesAsync(uriString, videoPath, showToast)
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

    /** 与横屏齿轮按钮相同：弹出播放器设置。竖屏底栏「更多」亦指向此处。 */
    private fun switchSessionVideo(item: VideoItem, onSwitched: () -> Unit = {}) {
        quickDialogs.dismissSubtitleSettingsSheet()
        firstFrames.showForNewMedia()
        preApplyOrientationForItem(item)
        viewModel.switchToVideo(item) {
            resetPlaybackSessionForNewVideo()
            onSwitched()
        }
    }

    private fun setupControls() {
        controlsBinder.bind()
        seekBars.attach()
        playerEvents.attach()
        syncPlayPauseIcon()
        applyControlVisibility()
    }

    private fun resetPlaybackSessionForNewVideo() {
        val reset = PlayerVideoSwitchPolicy.resetForNewVideo()
        abLoop.reset(reset)
        firstFrames.resetForNewVideo(reset.awaitFirstFrame)
        manualVideoZoom = reset.manualVideoZoom
        playbackWasBuffering = false
        playbackTicks.resetForNewVideo(reset)
        playerGestures.resetForNewVideo(reset)
    }

    private fun initGestures() = playerGestures.init()

    private fun applyScreenBrightness(adjustmentPercent: Int) =
        playerDisplay.applyScreenBrightness(adjustmentPercent)

    /**
     * 按屏宽/屏高比例微调横屏控件边距与运输区按钮尺寸（对齐 design/横屏播放界面 稿）。
     */
    private fun applyLandscapePlayerGeometry() = playerDisplay.applyLandscapePlayerGeometry()

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
        playbackTicks.observe()
    }

    private fun stopObservingState() {
        playbackTicks.stopObserving()
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

    private fun controlsChromeMaxAlpha(): Float =
        PlayerChromePolicy.maxChromeAlpha(playerPrefs.controlsOpacity)

    private fun showControls() = playerChrome.showControls()

    private fun hideControls() = playerChrome.hideControls()

    private fun showLockedControls() = playerChrome.showLockedControls()

    private fun applyControlVisibility() = playerChrome.applyControlVisibility()

    private fun hideChromeForSettingsOverlay() = playerChrome.hideChromeForSettingsOverlay()

    private fun restoreChromeAfterSettingsOverlay() = playerChrome.restoreChromeAfterSettingsOverlay()

    private fun scheduleHideControls() = playerChrome.scheduleHideControls()

    private fun applyChromePresentation(presentation: PlayerChromePresentation) =
        playerChrome.applyChromePresentation(presentation)

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

    private fun toggleScreenLock() = playerChrome.toggleScreenLock()

    private fun applyScreenLockChromeReveal() = playerChrome.applyScreenLockChromeReveal()

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

    private fun unlockPlayerForPause() = playerChrome.unlockPlayerForPause()

    private fun finishPlayer() = playerExit.finishPlayer()

    private fun preparePlayerExitFrame() = playerExit.preparePlayerExitFrame()

    private fun releasePlayerAfterExit() = playerExit.releasePlayerAfterExit()

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

        PlayerSystemUiController.enterImmersiveMode(this)
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
        if (playerExit.isFinishing) {
            preparePlayerExitFrame()
        }
        controlsContainer.post { applyLandscapePlayerGeometry() }
    }

    override fun onResume() {
        super.onResume()
        playerLifecycle.onResume()
    }

    override fun onPause() {
        super.onPause()
        playerLifecycle.onPause()
    }

    override fun onDestroy() {
        playbackNotifications.dismiss()
        playbackCoordinator.clearHandlers()
        subtitles.unregisterPrefsListener()
        handler.removeCallbacksAndMessages(null)
        playerEvents.detach()
        preparePlayerExitFrame()
        releasePlayerAfterExit()
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playerPip.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
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

    private fun enterPipModeIfSupported() = playerPip.enterIfSupported()

    private fun isInPipModeCompat(): Boolean = playerPip.isInPipModeCompat()

    companion object {
        private const val TAG_CONTENT_FRAME = "OVContentFrame"
        const val EXTRA_VIDEO_WIDTH = "video_width"
        const val EXTRA_VIDEO_HEIGHT = "video_height"
        const val EXTRA_START_POSITION_MS = "start_position_ms"
        const val EXTRA_FROM_PLAYBACK_NOTIFICATION = "from_playback_notification"
    }
}
