package com.example.openvideo.ui.player

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.openvideo.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SubtitleBgStyle
import com.google.android.material.button.MaterialButton
import android.widget.SeekBar
import android.widget.TextView

@AndroidEntryPoint
class PlayerSubtitleSettingsActivity : ComponentActivity() {

    @Inject lateinit var playerPrefs: PlayerPrefs

    private val pickSubtitleLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = Intent().apply {
                putExtra("subtitle_uri", uri.toString())
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_subtitle_settings)

        val btnLoad = findViewById<MaterialButton>(R.id.btn_load_subtitle)
        val tvSize = findViewById<TextView>(R.id.tv_subtitle_size_value)
        val sbSize = findViewById<SeekBar>(R.id.sb_subtitle_size)
        val tvBg = findViewById<TextView>(R.id.tv_subtitle_bg_value)
        val sbPosition = findViewById<SeekBar>(R.id.sb_subtitle_position)
        val tvEncoding = findViewById<TextView>(R.id.tv_encoding_value)
        val tvPreview = findViewById<TextView>(R.id.tv_subtitle_preview)
        val tvSecondaryPreview = findViewById<TextView>(R.id.tv_secondary_subtitle_preview)
        val tvSecondarySize = findViewById<TextView>(R.id.tv_secondary_subtitle_size_value)
        val sbSecondarySize = findViewById<SeekBar>(R.id.sb_secondary_subtitle_size)
        val tvSecondaryBg = findViewById<TextView>(R.id.tv_secondary_subtitle_bg_value)
        val sbSecondaryPosition = findViewById<SeekBar>(R.id.sb_secondary_subtitle_position)
        val tvSubtitleDelay = findViewById<TextView>(R.id.tv_subtitle_delay_value)
        val btnSubtitleDelayMinus = findViewById<MaterialButton>(R.id.btn_subtitle_delay_minus)
        val btnSubtitleDelayPlus = findViewById<MaterialButton>(R.id.btn_subtitle_delay_plus)
        val btnSubtitleDelayReset = findViewById<MaterialButton>(R.id.btn_subtitle_delay_reset)

        fun updateSubtitlePreview(position: Float = playerPrefs.subtitlePosition) {
            PlayerSubtitleSettingsPreviewPolicy.apply(
                preview = tvPreview,
                sampleText = getString(R.string.player_settings_subtitle_preview_sample),
                sizeSp = playerPrefs.subtitleSize,
                textColor = playerPrefs.subtitleColor,
                bgStyle = playerPrefs.subtitleBgStyle,
                position = position
            )
        }

        fun updateSecondarySubtitlePreview(position: Float = playerPrefs.secondarySubtitlePosition) {
            PlayerSubtitleSettingsPreviewPolicy.apply(
                preview = tvSecondaryPreview,
                sampleText = getString(R.string.player_settings_secondary_subtitle_preview_sample),
                sizeSp = playerPrefs.secondarySubtitleSize,
                textColor = playerPrefs.secondarySubtitleColor,
                bgStyle = playerPrefs.secondarySubtitleBgStyle,
                position = position
            )
        }

        fun updateSubtitleDelayText() {
            tvSubtitleDelay.text = getString(R.string.player_settings_unit_ms, playerPrefs.subtitleDelayMs)
        }

        btnLoad.setOnClickListener {
            pickSubtitleLauncher.launch(arrayOf("*/*"))
        }
        btnSubtitleDelayMinus.setOnClickListener {
            playerPrefs.subtitleDelayMs -= 500
            updateSubtitleDelayText()
        }
        btnSubtitleDelayPlus.setOnClickListener {
            playerPrefs.subtitleDelayMs += 500
            updateSubtitleDelayText()
        }
        btnSubtitleDelayReset.setOnClickListener {
            playerPrefs.subtitleDelayMs = 0
            updateSubtitleDelayText()
        }
        updateSubtitleDelayText()
        updateSubtitlePreview()
        updateSecondarySubtitlePreview()

        // Subtitle size
        val currentSize = playerPrefs.subtitleSize
        sbSize.progress = (currentSize - 14) / 2
        tvSize.text = "${currentSize}sp"
        var pendingSubtitleSize = currentSize
        sbSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val size = 14 + progress * 2
                tvSize.text = "${size}sp"
                if (fromUser) pendingSubtitleSize = size
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.subtitleSize = pendingSubtitleSize
                updateSubtitlePreview()
            }
        })

        PlayerSubtitleColorSwatchBinder.bind(
            root = findViewById(R.id.subtitle_color_swatch_row),
            playerPrefs = playerPrefs,
            density = resources.displayMetrics.density,
            onColorChanged = { updateSubtitlePreview() }
        )

        // Subtitle background
        val subtitleBgStyles = SubtitleBgStyle.entries.toTypedArray()
        fun subtitleBgLabel(style: SubtitleBgStyle): String = when (style) {
            SubtitleBgStyle.NONE -> getString(R.string.settings_subtitle_bg_none)
            SubtitleBgStyle.SEMI_TRANSPARENT -> getString(R.string.settings_subtitle_bg_semi)
            SubtitleBgStyle.OPAQUE -> getString(R.string.settings_subtitle_bg_opaque)
        }
        var bgIndex = subtitleBgStyles.indexOf(playerPrefs.subtitleBgStyle).takeIf { it >= 0 } ?: 1
        fun updateBgText() {
            tvBg.text = subtitleBgLabel(subtitleBgStyles[bgIndex])
        }
        updateBgText()
        tvBg.setOnClickListener {
            bgIndex = (bgIndex + 1) % subtitleBgStyles.size
            playerPrefs.subtitleBgStyle = subtitleBgStyles[bgIndex]
            updateBgText()
            updateSubtitlePreview()
        }

        // Subtitle position
        var pendingSubtitlePosition = playerPrefs.subtitlePosition
        sbPosition.progress = (pendingSubtitlePosition * 100).toInt()
        sbPosition.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pendingSubtitlePosition = progress / 100f
                    updateSubtitlePreview(pendingSubtitlePosition)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.subtitlePosition = pendingSubtitlePosition
                updateSubtitlePreview(pendingSubtitlePosition)
            }
        })

        val currentSecondarySize = playerPrefs.secondarySubtitleSize
        sbSecondarySize.progress = (currentSecondarySize - 14) / 2
        tvSecondarySize.text = "${currentSecondarySize}sp"
        var pendingSecondarySubtitleSize = currentSecondarySize
        sbSecondarySize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val size = 14 + progress * 2
                tvSecondarySize.text = "${size}sp"
                if (fromUser) pendingSecondarySubtitleSize = size
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.secondarySubtitleSize = pendingSecondarySubtitleSize
                updateSecondarySubtitlePreview()
            }
        })

        PlayerSubtitleColorSwatchBinder.bindSecondary(
            root = findViewById(R.id.secondary_subtitle_color_swatch_row),
            playerPrefs = playerPrefs,
            density = resources.displayMetrics.density,
            onColorChanged = { updateSecondarySubtitlePreview() }
        )

        var secondaryBgIndex = subtitleBgStyles.indexOf(playerPrefs.secondarySubtitleBgStyle).takeIf { it >= 0 } ?: 1
        fun updateSecondaryBgText() {
            tvSecondaryBg.text = subtitleBgLabel(subtitleBgStyles[secondaryBgIndex])
        }
        updateSecondaryBgText()
        tvSecondaryBg.setOnClickListener {
            secondaryBgIndex = (secondaryBgIndex + 1) % subtitleBgStyles.size
            playerPrefs.secondarySubtitleBgStyle = subtitleBgStyles[secondaryBgIndex]
            updateSecondaryBgText()
            updateSecondarySubtitlePreview()
        }

        var pendingSecondarySubtitlePosition = playerPrefs.secondarySubtitlePosition
        sbSecondaryPosition.progress = (pendingSecondarySubtitlePosition * 100).toInt()
        sbSecondaryPosition.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pendingSecondarySubtitlePosition = progress / 100f
                    updateSecondarySubtitlePreview(pendingSecondarySubtitlePosition)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.secondarySubtitlePosition = pendingSecondarySubtitlePosition
                updateSecondarySubtitlePreview(pendingSecondarySubtitlePosition)
            }
        })

        // Subtitle encoding
        val encodings = arrayOf("auto", "UTF-8", "GBK", "GB2312", "Big5", "Shift_JIS", "EUC-KR")
        var encIndex = encodings.indexOf(playerPrefs.subtitleEncoding).takeIf { it >= 0 } ?: 0
        fun updateEncText() {
            tvEncoding.text = if (encIndex == 0) getString(R.string.settings_encoding_auto) else encodings[encIndex]
        }
        updateEncText()
        tvEncoding.setOnClickListener {
            encIndex = (encIndex + 1) % encodings.size
            playerPrefs.subtitleEncoding = encodings[encIndex]
            updateEncText()
        }
    }
}
