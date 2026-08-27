package com.example.openvideo.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.openvideo.R
import dagger.hilt.android.AndroidEntryPoint
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

@AndroidEntryPoint
class CompatibilityPlayerActivity : AppCompatActivity() {
    private val viewModel: CompatibilityPlayerViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { setControlsVisible(false) }

    private lateinit var request: CompatibilityPlaybackRequest
    private lateinit var videoLayout: VLCVideoLayout
    private lateinit var controls: View
    private lateinit var loading: ProgressBar
    private lateinit var errorOverlay: View
    private lateinit var playPause: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private var viewsAttached = false
    private var mediaPrepared = false
    private var initialSeekPending = true
    private var userSeeking = false
    private var lastPositionMs = 0L
    private var durationMs = 0L
    private var resumeOnStart = true
    private var mediaFileDescriptor: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        request = CompatibilityPlayerContract.read(intent) ?: run {
            finish()
            return
        }
        lastPositionMs = savedInstanceState?.getLong(STATE_POSITION_MS) ?: request.startPositionMs
        durationMs = savedInstanceState?.getLong(STATE_DURATION_MS) ?: request.durationMs
        viewModel.initialize(request)

        setContentView(R.layout.activity_compatibility_player)
        PlayerSystemUiController.enterImmersiveMode(this)
        bindViews()
        createPlayer()
        setupControls()
        renderTimeline(lastPositionMs, durationMs)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
    }

    private fun bindViews() {
        videoLayout = findViewById(R.id.compatibility_video_layout)
        controls = findViewById(R.id.compatibility_controls)
        loading = findViewById(R.id.compatibility_loading)
        errorOverlay = findViewById(R.id.compatibility_error)
        playPause = findViewById(R.id.compatibility_play_pause)
        seekBar = findViewById(R.id.compatibility_seek)
        currentTime = findViewById(R.id.compatibility_current_time)
        totalTime = findViewById(R.id.compatibility_total_time)
        findViewById<TextView>(R.id.compatibility_title).text = request.title
        findViewById<View>(R.id.compatibility_root).setOnClickListener {
            setControlsVisible(controls.visibility != View.VISIBLE)
        }
    }

    private fun createPlayer() {
        libVlc = LibVLC(
            this,
            arrayListOf(
                "--no-stats",
                "--drop-late-frames",
                "--skip-frames",
                "--network-caching=1500"
            )
        )
        mediaPlayer = MediaPlayer(libVlc).apply {
            setEventListener(::onPlayerEvent)
        }
    }

    private fun prepareMediaIfNeeded() {
        if (mediaPrepared) return
        val media = runCatching {
            if (request.uri.scheme.equals("content", ignoreCase = true)) {
                val descriptor = contentResolver.openFileDescriptor(request.uri, "r")
                    ?: error("Content provider did not return a file descriptor")
                mediaFileDescriptor = descriptor
                Media(libVlc, descriptor.fileDescriptor)
            } else {
                Media(libVlc, request.uri)
            }
        }.getOrElse {
            showCompatibilityError()
            mediaPrepared = true
            return
        }.apply {
            // Compatibility mode may fall back internally, but never forces an unknown hardware decoder.
            setHWDecoderEnabled(true, false)
            request.requestHeaders.header("User-Agent")?.let {
                addOption(":http-user-agent=${it.asVlcOptionValue()}")
            }
            request.requestHeaders.header("Referer")?.let {
                addOption(":http-referrer=${it.asVlcOptionValue()}")
            }
        }
        mediaPlayer.media = media
        media.release()
        mediaPrepared = true
    }

    private fun setupControls() {
        findViewById<View>(R.id.compatibility_back).setOnClickListener { finish() }
        playPause.setOnClickListener { togglePlayback() }
        findViewById<View>(R.id.compatibility_rewind).setOnClickListener { seekBy(-10_000L) }
        findViewById<View>(R.id.compatibility_forward).setOnClickListener { seekBy(10_000L) }
        findViewById<View>(R.id.compatibility_error_back).setOnClickListener { finish() }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                userSeeking = true
                handler.removeCallbacks(hideControlsRunnable)
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && durationMs > 0L) {
                    val target = durationMs * progress / seekBar.max
                    currentTime.text = PlayerTimeFormatter.format(target)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (durationMs > 0L && mediaPlayer.isSeekable) {
                    mediaPlayer.setTime(durationMs * seekBar.progress / seekBar.max, true)
                }
                userSeeking = false
                scheduleHideControls()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (!this::mediaPlayer.isInitialized) return
        if (!viewsAttached) {
            mediaPlayer.attachViews(videoLayout, null, true, false)
            viewsAttached = true
        }
        prepareMediaIfNeeded()
        if (resumeOnStart && mediaPlayer.hasMedia()) mediaPlayer.play()
        scheduleHideControls()
    }

    override fun onStop() {
        if (!this::mediaPlayer.isInitialized) {
            super.onStop()
            return
        }
        resumeOnStart = mediaPlayer.isPlaying
        updateHistory(mediaPlayer.time, mediaPlayer.length)
        if (mediaPlayer.isPlaying) mediaPlayer.pause()
        if (viewsAttached) {
            mediaPlayer.detachViews()
            viewsAttached = false
        }
        viewModel.saveHistory()
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (this::mediaPlayer.isInitialized) {
            mediaPlayer.setEventListener(null)
            mediaPlayer.stop()
            mediaPlayer.release()
        }
        if (this::libVlc.isInitialized) libVlc.release()
        mediaFileDescriptor?.close()
        mediaFileDescriptor = null
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(STATE_POSITION_MS, lastPositionMs)
        outState.putLong(STATE_DURATION_MS, durationMs)
        super.onSaveInstanceState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_SPACE -> {
            togglePlayback()
            true
        }
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_MEDIA_REWIND -> {
            seekBy(-10_000L)
            true
        }
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
            seekBy(10_000L)
            true
        }
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_MENU -> {
            setControlsVisible(true)
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun onPlayerEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Opening -> loading.visibility = View.VISIBLE
            MediaPlayer.Event.Buffering -> loading.visibility =
                if (event.buffering < 100f) View.VISIBLE else View.GONE
            MediaPlayer.Event.Playing -> {
                loading.visibility = View.GONE
                errorOverlay.visibility = View.GONE
                if (initialSeekPending) {
                    if (lastPositionMs > 0L) mediaPlayer.setTime(lastPositionMs, false)
                    initialSeekPending = false
                }
                if (request.speed != 1f) mediaPlayer.rate = request.speed
                if (request.audioMuted) mediaPlayer.volume = 0
                updatePlayPauseIcon(true)
            }
            MediaPlayer.Event.Paused,
            MediaPlayer.Event.Stopped -> updatePlayPauseIcon(false)
            MediaPlayer.Event.TimeChanged -> updateHistory(event.timeChanged, mediaPlayer.length)
            MediaPlayer.Event.LengthChanged -> updateHistory(mediaPlayer.time, event.lengthChanged)
            MediaPlayer.Event.EndReached -> {
                viewModel.markEnded()
                updatePlayPauseIcon(false)
                setControlsVisible(true)
            }
            MediaPlayer.Event.EncounteredError -> {
                showCompatibilityError()
            }
            MediaPlayer.Event.Vout -> videoLayout.post { mediaPlayer.updateVideoSurfaces() }
        }
    }

    private fun updateHistory(positionMs: Long, durationMs: Long) {
        lastPositionMs = positionMs.coerceAtLeast(0L)
        if (durationMs > 0L) this.durationMs = durationMs
        viewModel.updatePlayback(lastPositionMs, this.durationMs)
        if (!userSeeking) renderTimeline(lastPositionMs, this.durationMs)
    }

    private fun showCompatibilityError() {
        loading.visibility = View.GONE
        errorOverlay.visibility = View.VISIBLE
        controls.visibility = View.GONE
    }

    private fun renderTimeline(positionMs: Long, durationMs: Long) {
        currentTime.text = PlayerTimeFormatter.format(positionMs)
        totalTime.text = PlayerTimeFormatter.format(durationMs)
        seekBar.progress = if (durationMs > 0L) {
            ((positionMs.coerceAtMost(durationMs) * seekBar.max) / durationMs).toInt()
        } else {
            0
        }
    }

    private fun togglePlayback() {
        if (mediaPlayer.isPlaying) mediaPlayer.pause() else mediaPlayer.play()
        updatePlayPauseIcon(mediaPlayer.isPlaying)
        setControlsVisible(true)
    }

    private fun seekBy(deltaMs: Long) {
        if (!mediaPlayer.isSeekable) return
        mediaPlayer.setTime((mediaPlayer.time + deltaMs).coerceIn(0L, mediaPlayer.length.coerceAtLeast(0L)), true)
        setControlsVisible(true)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        playPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun setControlsVisible(visible: Boolean) {
        controls.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) scheduleHideControls() else handler.removeCallbacks(hideControlsRunnable)
    }

    private fun scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        if (mediaPlayer.isPlaying) handler.postDelayed(hideControlsRunnable, CONTROLS_TIMEOUT_MS)
    }

    private fun Map<String, String>.header(name: String): String? =
        entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    private fun String.asVlcOptionValue(): String = replace("\r", "").replace("\n", "")

    companion object {
        private const val STATE_POSITION_MS = "compatibility_state_position"
        private const val STATE_DURATION_MS = "compatibility_state_duration"
        private const val CONTROLS_TIMEOUT_MS = 3_500L
    }
}
