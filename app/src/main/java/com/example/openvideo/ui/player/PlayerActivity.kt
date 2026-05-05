package com.example.openvideo.ui.player

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.openvideo.R
import com.example.openvideo.core.player.AspectRatio
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.prefs.PlayerPrefs
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()
        setContentView(R.layout.activity_player)

        initViews()
        initGestures()
        setupControls()
        applyPlayerSettings()

        val uriString = intent.getStringExtra("video_uri") ?: run { finish(); return }
        val title = intent.getStringExtra("video_title") ?: ""
        val id = intent.getLongExtra("video_id", 0)

        viewModel.initialize(Uri.parse(uriString), title, id)

        playerView.player = viewModel.player
        tvTitle.text = title

        observeState()
        scheduleHideControls()

        // Restore playback position if remember_progress is on
        if (playerPrefs.rememberProgress) {
            viewModel.restorePosition(id)
        }
    }

    private fun applyPlayerSettings() {
        // Apply speed
        viewModel.setSpeed(playerPrefs.speed)

        // Apply keep screen on
        if (playerPrefs.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun loadSubtitles(videoPath: String) {
        val subtitleFiles = subtitleLoader.findSubtitleFiles(videoPath)
        if (subtitleFiles.isNotEmpty()) {
            // Load the first subtitle file found
            val subtitles = subtitleLoader.loadFromFile(subtitleFiles[0])
            if (subtitles.isNotEmpty()) {
                viewModel.setSubtitles(subtitles)
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
            PlayerSettingsDialog(this, playerManager, viewModel, playerPrefs).show()
        }

        btnScreenshot.setOnClickListener {
            val surfaceView = playerView.videoSurfaceView as? android.view.SurfaceView
            if (surfaceView != null) {
                playerManager.takeScreenshot(surfaceView) { success, path ->
                    runOnUiThread {
                        if (success) {
                            android.widget.Toast.makeText(this, "截图已保存: $path", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(this, "截图失败", android.widget.Toast.LENGTH_SHORT).show()
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
                    android.widget.Toast.makeText(this, "A点已设置: ${formatTime(abLoopPointA)}", android.widget.Toast.LENGTH_SHORT).show()
                }
                AbLoopState.POINT_A_SET -> {
                    abLoopPointB = playerManager.currentPosition
                    if (abLoopPointB > abLoopPointA) {
                        abLoopState = AbLoopState.LOOPING
                        btnAbLoop.setColorFilter(android.graphics.Color.GREEN)
                        android.widget.Toast.makeText(this, "B点已设置，开始循环", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        abLoopState = AbLoopState.IDLE
                        abLoopPointA = -1
                        btnAbLoop.clearColorFilter()
                        android.widget.Toast.makeText(this, "B点必须在A点之后", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                AbLoopState.LOOPING -> {
                    abLoopState = AbLoopState.IDLE
                    abLoopPointA = -1
                    abLoopPointB = -1
                    btnAbLoop.clearColorFilter()
                    android.widget.Toast.makeText(this, "AB循环已取消", android.widget.Toast.LENGTH_SHORT).show()
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
                viewModel.seekTo(sb.progress.toLong())
                isSeeking = false
                scheduleHideControls()
            }
        })

        controlsContainer.setOnClickListener {
            if (controlsVisible) hideControls() else showControls()
        }

        viewModel.player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon(isPlaying)
            }
        })
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

        gestureOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    isHorizontalSwipe = false
                    isVerticalSwipe = false
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
                        if (abs(dx) > 50 || abs(dy) > 50) {
                            if (abs(dx) > abs(dy)) {
                                isHorizontalSwipe = true
                            } else {
                                isVerticalSwipe = true
                            }
                        }
                    }

                    if (isHorizontalSwipe) {
                        handleSeekGesture(dx)
                    } else if (isVerticalSwipe) {
                        when (swipeSide) {
                            SwipeSide.LEFT -> handleBrightnessGesture(dy)
                            SwipeSide.RIGHT -> handleVolumeGesture(dy)
                            SwipeSide.NONE -> {}
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // P1: Edge swipe back
                    if (isEdgeSwipe && isHorizontalSwipe) {
                        val dx = event.x - startX
                        if (dx > 100) { // Swipe right from left edge
                            finish()
                            return@setOnTouchListener true
                        }
                    }

                    if (isHorizontalSwipe) {
                        applySeekGesture()
                    }
                    seekIndicator.visibility = View.GONE
                    brightnessIndicator.visibility = View.GONE
                    volumeIndicator.visibility = View.GONE

                    // P1: Restore speed after long press
                    if (playerPrefs.longPressAction == com.example.openvideo.core.prefs.LongPressAction.SPEED) {
                        viewModel.setSpeed(playerPrefs.speed)
                    }
                }
            }
            true
        }
    }

    private fun handleSeekGesture(dx: Float) {
        val seekMs = (dx / resources.displayMetrics.widthPixels * 60_000).toLong()
        val target = (viewModel.uiState.value.currentPosition + seekMs)
            .coerceIn(0, viewModel.uiState.value.duration)
        seekIndicator.text = "${formatTime(target)} / ${formatTime(viewModel.uiState.value.duration)}"
        seekIndicator.visibility = View.VISIBLE
    }

    private fun applySeekGesture() {
        // The seek is already shown as indicator, actual seek happens on ACTION_UP
    }

    private fun handleBrightnessGesture(dy: Float) {
        val delta = -dy / resources.displayMetrics.heightPixels
        currentBrightness = (currentBrightness + delta).coerceIn(0.01f, 1f)
        val layoutParams = window.attributes
        layoutParams.screenBrightness = currentBrightness
        window.attributes = layoutParams
        brightnessProgress.progress = (currentBrightness * 100).toInt()
        brightnessIndicator.visibility = View.VISIBLE
    }

    private fun handleVolumeGesture(dy: Float) {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val delta = -dy / resources.displayMetrics.heightPixels * maxVolume
        currentVolume = (currentVolume + delta / maxVolume).coerceIn(0f, 1f)
        val volume = (currentVolume * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
        volumeProgress.progress = (currentVolume * 100).toInt()
        volumeIndicator.visibility = View.VISIBLE
    }

    private fun observeState() {
        val updateRunnable = object : Runnable {
            override fun run() {
                if (!isSeeking) {
                    viewModel.updatePosition()
                    val state = viewModel.uiState.value
                    seekBar.max = state.duration.toInt()
                    seekBar.progress = state.currentPosition.toInt()
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
        handler.post(updateRunnable)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        val icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        btnPlay.setImageResource(icon)
        btnPlayCenter.setImageResource(icon)
        btnPlayCenter.visibility = if (isPlaying) View.GONE else View.VISIBLE
    }

    private fun showControls() {
        controlsVisible = true
        controlsContainer.animate().alpha(1f).setDuration(200).start()
        controlsContainer.visibility = View.VISIBLE
        scheduleHideControls()
    }

    private fun hideControls() {
        controlsVisible = false
        controlsContainer.animate().alpha(0f).setDuration(200).withEndAction {
            controlsContainer.visibility = View.GONE
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
            // Lock screen - disable touch on gesture overlay
            gestureOverlay.setOnTouchListener { _, _ -> true }
            btnLock.setImageResource(R.drawable.ic_lock)
            btnLock.setColorFilter(android.graphics.Color.RED)
            android.widget.Toast.makeText(this, "屏幕已锁定，点击锁定按钮解锁", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            // Unlock screen - restore gesture handling
            initGestures()
            btnLock.setImageResource(R.drawable.ic_lock)
            btnLock.clearColorFilter()
            android.widget.Toast.makeText(this, "屏幕已解锁", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    override fun onPause() {
        super.onPause()
        if (!isInPictureInPictureMode) {
            viewModel.saveHistory()
            viewModel.player?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        viewModel.release()
    }

    override fun onPictureInPictureModeChanged(isInPipMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPipMode, newConfig)
        if (isInPipMode) {
            // Hide controls in PiP mode
            controlsContainer.visibility = View.GONE
        } else {
            // Show controls when exiting PiP
            controlsContainer.visibility = View.VISIBLE
        }
    }

    private enum class SwipeSide { LEFT, RIGHT, NONE }
    private enum class AbLoopState { IDLE, POINT_A_SET, LOOPING }
}
