package com.example.openvideo.ui.player

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.openvideo.R
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.GestureAction
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SubtitleBgStyle
import com.example.openvideo.core.subtitle.SubtitleLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {

    @Inject lateinit var playerManager: PlayerManager
    @Inject lateinit var playerPrefs: PlayerPrefs
    @Inject lateinit var subtitleLoader: SubtitleLoader
    private val viewModel: PlayerViewModel by viewModels()

    private lateinit var playerView: PlayerView
    private lateinit var controlsContainer: View
    private lateinit var btnPlay: ImageButton
    private lateinit var btnPlayCenter: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnScreenshot: ImageButton
    private lateinit var btnAbLoop: ImageButton
    private lateinit var btnPip: ImageButton
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

    private val handler = Handler(Looper.getMainLooper())
    private var controlsVisible = true
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

                // AB Loop logic
                if (abLoopState == AbLoopState.LOOPING && abLoopPointA >= 0 && abLoopPointB >= 0) {
                    if (state.currentPosition >= abLoopPointB) {
                        playerManager.seekTo(abLoopPointA)
                    }
                }

                // Update subtitle
                val subtitle = viewModel.getCurrentSubtitle()
                if (subtitle.isNotEmpty()) {
                    tvSubtitle.text = subtitle
                    tvSubtitle.visibility = View.VISIBLE
                } else {
                    tvSubtitle.visibility = View.GONE
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    private lateinit var gestureDetector: GestureDetector
    private var currentBrightness = 0.5f
    private var currentVolume = 0.5f
    private var isSeeking = false

    // AB Loop state
    private var abLoopState = AbLoopState.IDLE
    private var abLoopPointA: Long = -1
    private var abLoopPointB: Long = -1

    // Screen lock state
    private var isScreenLocked = false
    private var pendingSeekTarget: Long? = null
    private var playerListener: Player.Listener? = null
    private var isLongPressing = false
    private var hasSkippedIntro = false

    /** 单次手势起始亮度/音量（0–1），避免 MOVE 期间重复累加误差 */
    private var brightnessGestureAnchor = 0.5f
    private var volumeGestureAnchor = 0.5f

    private lateinit var pickSubtitleLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        enterImmersiveMode()
        setContentView(R.layout.activity_player)

        initViews()
        initGestures()
        setupControls()
        initBrightnessAndVolume()
        applyPlayerSettings()

        val uriString = intent.getStringExtra("video_uri") ?: run { finish(); return }
        val title = intent.getStringExtra("video_title") ?: ""
        val id = intent.getLongExtra("video_id", 0)

        viewModel.initialize(Uri.parse(uriString), title, id)

        // Load external subtitles if available
        loadSubtitles(uriString)

        playerView.player = viewModel.player
        tvTitle.text = title

        scheduleHideControls()

        // Restore playback position if remember_progress is on
        if (playerPrefs.rememberProgress) {
            viewModel.restorePosition(id)
        }
    }

    private fun applyPlayerSettings() {
        viewModel.setSpeed(
            playerPrefs.speed,
            PlayerPlaybackSettings.pitchFor(playerPrefs.speed, playerPrefs.speedPreservePitch)
        )
        viewModel.setRepeatMode(PlayerPlaybackSettings.repeatModeFor(playerPrefs.loopMode))
        viewModel.setVolumeBoost(playerPrefs.volumeBoost)

        if (playerPrefs.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        playerView.resizeMode = PlayerViewSettings.resizeModeFor(playerPrefs.aspectRatio)
        playerView.rotation = playerPrefs.rotation.toFloat()
        playerView.scaleX = if (playerPrefs.mirror) -1f else 1f

        tvSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, playerPrefs.subtitleSize.toFloat())
        tvSubtitle.setTextColor(playerPrefs.subtitleColor)
        tvSubtitle.setBackgroundColor(
            when (playerPrefs.subtitleBgStyle) {
                SubtitleBgStyle.NONE -> Color.TRANSPARENT
                SubtitleBgStyle.SEMI_TRANSPARENT -> Color.argb(170, 0, 0, 0)
                SubtitleBgStyle.OPAQUE -> Color.BLACK
            }
        )
        tvSubtitle.post {
            val travel = playerView.height * 0.6f
            tvSubtitle.translationY = -((1f - playerPrefs.subtitlePosition.coerceIn(0f, 1f)) * travel)
        }
    }

    private fun initBrightnessAndVolume() {
        // Read current window brightness (BRIGHTNESS_DEFAULT is -1f)
        val windowBrightness = window.attributes.screenBrightness
        currentBrightness = if (windowBrightness in 0f..1f) {
            windowBrightness
        } else {
            // Read system brightness setting
            try {
                android.provider.Settings.System.getInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS
                ) / 255f
            } catch (_: Exception) {
                0.5f
            }
        }

        // Read current music stream volume
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        currentVolume = if (maxVolume > 0) currentVol.toFloat() / maxVolume else 0.5f

        brightnessProgress.progress = (currentBrightness * 100).toInt()
        volumeProgress.progress = (currentVolume * 100).toInt()
    }

    private fun loadSubtitles(uriString: String) {
        val uri = Uri.parse(uriString)
        when (uri.scheme) {
            "file" -> {
                val path = uri.path ?: return
                val subtitleFiles = subtitleLoader.findSubtitleFiles(path)
                if (subtitleFiles.isNotEmpty()) {
                    val subtitles = subtitleLoader.loadFromFile(subtitleFiles[0])
                    if (subtitles.isNotEmpty()) {
                        viewModel.setSubtitles(subtitles)
                    }
                }
            }
            else -> {
                // content:// or other schemes — try loading directly from URI
                val subtitles = subtitleLoader.loadFromUri(uri)
                if (subtitles.isNotEmpty()) {
                    viewModel.setSubtitles(subtitles)
                }
            }
        }
    }

    private fun initViews() {
        playerView = findViewById(R.id.player_view)
        controlsContainer = findViewById(R.id.controls_container)
        btnPlay = findViewById(R.id.btn_play)
        btnPlayCenter = findViewById(R.id.btn_play_center)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        btnSettings = findViewById(R.id.btn_settings)
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
    }

    private fun setupControls() {
        btnPlay.setOnClickListener { viewModel.togglePlayPause() }
        btnPlayCenter.setOnClickListener { viewModel.togglePlayPause() }
        btnBack.setOnClickListener { finish() }

        btnPrev.setOnClickListener { viewModel.seekBackward() }
        btnNext.setOnClickListener { viewModel.seekForward() }

        btnSettings.setOnClickListener {
            val dialog = PlayerSettingsDialog(this, playerManager, viewModel, playerPrefs) {
                pickSubtitleLauncher.launch(arrayOf("*/*"))
            }
            dialog.setOnDismissListener {
                applyPlayerSettings()
                scheduleHideControls()
            }
            dialog.show()
        }

        btnScreenshot.setOnClickListener {
            val surfaceView = playerView.videoSurfaceView as? android.view.SurfaceView
            if (surfaceView != null) {
                playerManager.takeScreenshot(surfaceView) { success, path ->
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

        btnAbLoop.setOnClickListener {
            when (abLoopState) {
                AbLoopState.IDLE -> {
                    abLoopPointA = playerManager.currentPosition
                    abLoopState = AbLoopState.POINT_A_SET
                    btnAbLoop.setColorFilter(android.graphics.Color.YELLOW)
                    android.widget.Toast.makeText(this, getString(R.string.player_ab_point_a_set, formatTime(abLoopPointA)), android.widget.Toast.LENGTH_SHORT).show()
                }
                AbLoopState.POINT_A_SET -> {
                    abLoopPointB = playerManager.currentPosition
                    if (abLoopPointB > abLoopPointA) {
                        abLoopState = AbLoopState.LOOPING
                        btnAbLoop.setColorFilter(android.graphics.Color.GREEN)
                        android.widget.Toast.makeText(this, getString(R.string.player_ab_loop_started), android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        abLoopState = AbLoopState.IDLE
                        abLoopPointA = -1
                        btnAbLoop.clearColorFilter()
                        android.widget.Toast.makeText(this, getString(R.string.player_ab_point_b_error), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                AbLoopState.LOOPING -> {
                    abLoopState = AbLoopState.IDLE
                    abLoopPointA = -1
                    abLoopPointB = -1
                    btnAbLoop.clearColorFilter()
                    android.widget.Toast.makeText(this, getString(R.string.player_ab_loop_cancelled), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnFullscreen.setOnClickListener {
            requestedOrientation = if (resources.configuration.orientation == 1) {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        btnPip.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val params = android.app.PictureInPictureParams.Builder().build()
                enterPictureInPictureMode(params)
            }
        }

        btnLock.setOnClickListener {
            toggleScreenLock()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvCurrentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar) {
                isSeeking = true
                handler.removeCallbacks(hideControlsRunnable)
            }

            override fun onStopTrackingTouch(sb: SeekBar) {
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
            if (controlsVisible) hideControls() else showControls()
        }

        playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (
                    playbackState == Player.STATE_READY &&
                    playerPrefs.skipIntroOutro &&
                    playerPrefs.introSeconds > 0 &&
                    !hasSkippedIntro &&
                    viewModel.uiState.value.currentPosition < playerPrefs.introSeconds * 1000L
                ) {
                    hasSkippedIntro = true
                    viewModel.seekTo(playerPrefs.introSeconds * 1000L)
                }
            }
        }
        viewModel.player?.addListener(playerListener!!)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (controlsVisible) hideControls() else showControls()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                // P1: Double tap action from settings
                when (playerPrefs.doubleTapAction) {
                    com.example.openvideo.core.prefs.DoubleTapAction.PLAY_PAUSE -> viewModel.togglePlayPause()
                    com.example.openvideo.core.prefs.DoubleTapAction.FORWARD -> {
                        val ms = playerPrefs.seekInterval * 1000L
                        viewModel.seekForward(ms)
                    }
                    com.example.openvideo.core.prefs.DoubleTapAction.BACKWARD -> {
                        val ms = playerPrefs.seekInterval * 1000L
                        viewModel.seekBackward(ms)
                    }
                    com.example.openvideo.core.prefs.DoubleTapAction.NONE -> {}
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                // P1: Long press action from settings
                when (playerPrefs.longPressAction) {
                    com.example.openvideo.core.prefs.LongPressAction.SPEED -> {
                        isLongPressing = true
                        val speed = playerPrefs.longPressSpeed
                        viewModel.setSpeed(speed)
                    }
                    com.example.openvideo.core.prefs.LongPressAction.NONE -> {}
                }
            }
        })

        var startX = 0f
        var startY = 0f
        var isHorizontalSwipe = false
        var isVerticalSwipe = false
        var isEdgeSwipe = false
        var swipeSide = SwipeSide.NONE
        val edgeThreshold = resources.displayMetrics.widthPixels * 0.05f // 5% edge
        val gestureSlop = gestureSlopPx()

        gestureOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isHorizontalSwipe = false
                    isVerticalSwipe = false
                    brightnessGestureAnchor = currentBrightness
                    volumeGestureAnchor = currentVolume
                    isEdgeSwipe = event.x < edgeThreshold || event.x > resources.displayMetrics.widthPixels - edgeThreshold
                    swipeSide = if (event.x < resources.displayMetrics.widthPixels / 2) {
                        SwipeSide.LEFT
                    } else {
                        SwipeSide.RIGHT
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - startX
                    val dy = event.y - startY

                    if (!isHorizontalSwipe && !isVerticalSwipe) {
                        if (abs(dx) > gestureSlop || abs(dy) > gestureSlop) {
                            if (abs(dx) > abs(dy)) {
                                isHorizontalSwipe = true
                            } else {
                                isVerticalSwipe = true
                            }
                        }
                    }

                    if (isHorizontalSwipe) {
                        when (playerPrefs.horizontalSwipeAction) {
                            GestureAction.SEEK -> handleSeekGesture(dx)
                            GestureAction.BRIGHTNESS -> handleBrightnessGestureHorizontal(dx)
                            GestureAction.VOLUME -> handleVolumeGestureHorizontal(dx)
                            GestureAction.NONE -> {}
                        }
                    } else if (isVerticalSwipe) {
                        val action = when (swipeSide) {
                            SwipeSide.LEFT -> playerPrefs.leftVerticalGesture
                            SwipeSide.RIGHT -> playerPrefs.rightVerticalGesture
                            SwipeSide.NONE -> GestureAction.NONE
                        }
                        when (action) {
                            GestureAction.BRIGHTNESS -> handleBrightnessGesture(dy)
                            GestureAction.VOLUME -> handleVolumeGesture(dy)
                            GestureAction.SEEK -> handleVerticalSeekGesture(dy)
                            GestureAction.NONE -> {}
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // P1: Edge swipe back
                    if (playerPrefs.edgeSwipeBack && isEdgeSwipe && isHorizontalSwipe) {
                        val dx = event.x - startX
                        if (dx > 100) { // Swipe right from left edge
                            finish()
                            return@setOnTouchListener true
                        }
                    }

                    if (isHorizontalSwipe && playerPrefs.horizontalSwipeAction == GestureAction.SEEK) {
                        applySeekGesture()
                    } else if (isVerticalSwipe) {
                        val verticalAction = when (swipeSide) {
                            SwipeSide.LEFT -> playerPrefs.leftVerticalGesture
                            SwipeSide.RIGHT -> playerPrefs.rightVerticalGesture
                            SwipeSide.NONE -> GestureAction.NONE
                        }
                        if (verticalAction == GestureAction.SEEK) {
                            applySeekGesture()
                        }
                    }
                    seekIndicator.visibility = View.GONE
                    brightnessIndicator.visibility = View.GONE
                    volumeIndicator.visibility = View.GONE

                    // P1: Restore speed after long press
                    if (isLongPressing) {
                        isLongPressing = false
                        viewModel.setSpeed(playerPrefs.speed)
                    }
                }
            }
            true
        }
    }

    private fun gestureSlopPx(): Int = when (playerPrefs.gestureSensitivity) {
        1 -> 60
        2 -> 50
        else -> 40
    }

    private fun handleSeekGesture(dx: Float) {
        val seekMs = (dx / resources.displayMetrics.widthPixels * 60_000).toLong()
        val state = viewModel.uiState.value
        val target = PlayerTimeline.safeSeekTarget(state.currentPosition, seekMs, state.duration)
        pendingSeekTarget = target
        seekIndicator.text = "${formatTime(target)} / ${formatTime(state.duration)}"
        seekIndicator.visibility = View.VISIBLE
    }

    private fun handleVerticalSeekGesture(dy: Float) {
        val seekMs = (-dy / resources.displayMetrics.heightPixels * 60_000).toLong()
        val state = viewModel.uiState.value
        val target = PlayerTimeline.safeSeekTarget(state.currentPosition, seekMs, state.duration)
        pendingSeekTarget = target
        seekIndicator.text = "${formatTime(target)} / ${formatTime(state.duration)}"
        seekIndicator.visibility = View.VISIBLE
    }

    private fun applySeekGesture() {
        val state = viewModel.uiState.value
        // Re-read the target from the indicator text isn't reliable, recalculate
        // The target was computed in handleSeekGesture, store it instead
        pendingSeekTarget?.let { target ->
            viewModel.seekTo(PlayerTimeline.safeSeekTarget(0, target, state.duration))
            pendingSeekTarget = null
        }
    }

    private fun handleBrightnessGesture(dy: Float) {
        currentBrightness =
            (brightnessGestureAnchor - dy / resources.displayMetrics.heightPixels).coerceIn(0.01f, 1f)
        val layoutParams = window.attributes
        layoutParams.screenBrightness = currentBrightness
        window.attributes = layoutParams
        brightnessProgress.progress = (currentBrightness * 100).toInt()
        brightnessIndicator.visibility = View.VISIBLE
    }

    private fun handleBrightnessGestureHorizontal(dx: Float) {
        currentBrightness =
            (brightnessGestureAnchor + dx / resources.displayMetrics.widthPixels).coerceIn(0.01f, 1f)
        val layoutParams = window.attributes
        layoutParams.screenBrightness = currentBrightness
        window.attributes = layoutParams
        brightnessProgress.progress = (currentBrightness * 100).toInt()
        brightnessIndicator.visibility = View.VISIBLE
    }

    private fun handleVolumeGesture(dy: Float) {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        currentVolume =
            (volumeGestureAnchor - dy / resources.displayMetrics.heightPixels).coerceIn(0f, 1f)
        val volume = (currentVolume * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
        volumeProgress.progress = (currentVolume * 100).toInt()
        volumeIndicator.visibility = View.VISIBLE
    }

    private fun handleVolumeGestureHorizontal(dx: Float) {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        currentVolume =
            (volumeGestureAnchor + dx / resources.displayMetrics.widthPixels).coerceIn(0f, 1f)
        val volume = (currentVolume * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
        volumeProgress.progress = (currentVolume * 100).toInt()
        volumeIndicator.visibility = View.VISIBLE
    }

    private fun observeState() {
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    private fun stopObservingState() {
        handler.removeCallbacks(updateRunnable)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        val icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        btnPlay.setImageResource(icon)
        btnPlayCenter.setImageResource(icon)
        btnPlayCenter.visibility = if (isPlaying) View.GONE else View.VISIBLE
    }

    private fun showControls() {
        controlsVisible = true
        controlsContainer.animate().cancel()
        controlsContainer.alpha = 1f
        controlsContainer.visibility = View.VISIBLE
        scheduleHideControls()
    }

    private fun hideControls() {
        controlsVisible = false
        controlsContainer.animate().cancel()
        controlsContainer.animate().alpha(0f).setDuration(200).withEndAction {
            if (!controlsVisible) {
                controlsContainer.visibility = View.GONE
            }
        }.start()
    }

    private fun scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        val delay = playerPrefs.controlsAutoHide * 1000L
        if (delay > 0) {
            handler.postDelayed(hideControlsRunnable, delay)
        }
    }

    private fun toggleScreenLock() {
        isScreenLocked = !isScreenLocked
        if (isScreenLocked) {
            gestureOverlay.setOnTouchListener { _, _ -> true }
            btnLock.setColorFilter(android.graphics.Color.RED)
            android.widget.Toast.makeText(this, getString(R.string.player_locked), android.widget.Toast.LENGTH_SHORT).show()
        } else {
            initGestures()
            btnLock.clearColorFilter()
            android.widget.Toast.makeText(this, getString(R.string.player_unlocked), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    override fun onResume() {
        super.onResume()
        observeState()
    }

    override fun onPause() {
        super.onPause()
        stopObservingState()
        if (!isInPictureInPictureMode) {
            viewModel.saveHistory()
            if (playerPrefs.pauseOnExit || !playerPrefs.bgAudio) {
                viewModel.player?.pause()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        playerListener?.let { viewModel.player?.removeListener(it) }
        playerListener = null
        viewModel.release()
    }

    override fun onPictureInPictureModeChanged(isInPipMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPipMode, newConfig)
        if (isInPipMode) {
            handler.removeCallbacks(hideControlsRunnable)
            controlsContainer.animate().cancel()
            controlsContainer.alpha = 0f
            controlsContainer.visibility = View.GONE
            controlsVisible = false
        } else {
            showControls()
        }
    }

    private enum class SwipeSide { LEFT, RIGHT, NONE }
    private enum class AbLoopState { IDLE, POINT_A_SET, LOOPING }
}
