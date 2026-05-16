package com.example.openvideo.ui.player

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SubtitleBgStyle

@AndroidEntryPoint
class PlayerSubtitleSettingsSheet : BaseSettingsSheet() {
    override val layoutResId: Int = R.layout.activity_player_subtitle_settings

    @Inject lateinit var playerPrefs: PlayerPrefs

    private val pickSubtitleLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // store into prefs so PlayerActivity can pick it up
            playerPrefs.externalSubtitleUri = uri.toString()
            dismiss()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_player_subtitle_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLoad = view.findViewById<MaterialButton>(R.id.btn_load_subtitle)
        val tvSize = view.findViewById<TextView>(R.id.tv_subtitle_size_value)
        val sbSize = view.findViewById<SeekBar>(R.id.sb_subtitle_size)
        val tvBg = view.findViewById<TextView>(R.id.tv_subtitle_bg_value)
        val sbPosition = view.findViewById<SeekBar>(R.id.sb_subtitle_position)
        val tvEncoding = view.findViewById<TextView>(R.id.tv_encoding_value)
        val tvPreview = view.findViewById<TextView>(R.id.tv_subtitle_preview)
        val tvSubtitleDelay = view.findViewById<TextView>(R.id.tv_subtitle_delay_value)
        val btnSubtitleDelayMinus = view.findViewById<MaterialButton>(R.id.btn_subtitle_delay_minus)
        val btnSubtitleDelayPlus = view.findViewById<MaterialButton>(R.id.btn_subtitle_delay_plus)
        val btnSubtitleDelayReset = view.findViewById<MaterialButton>(R.id.btn_subtitle_delay_reset)

        fun updateSubtitlePreview() {
            tvPreview.text = getString(R.string.player_settings_subtitle_preview_sample)
            tvPreview.textSize = playerPrefs.subtitleSize.toFloat()
            tvPreview.setTextColor(playerPrefs.subtitleColor)
            tvPreview.setBackgroundColor(PlayerSubtitleStylePolicy.backgroundColor(playerPrefs.subtitleBgStyle))
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

        val subtitleBgStyles = SubtitleBgStyle.entries.toTypedArray()
        var bgIndex = subtitleBgStyles.indexOf(playerPrefs.subtitleBgStyle).takeIf { it >= 0 } ?: 1
        fun updateBgText() {
            tvBg.text = when (subtitleBgStyles[bgIndex]) {
                SubtitleBgStyle.NONE -> getString(R.string.settings_subtitle_bg_none)
                SubtitleBgStyle.SEMI_TRANSPARENT -> getString(R.string.settings_subtitle_bg_semi)
                SubtitleBgStyle.OPAQUE -> getString(R.string.settings_subtitle_bg_opaque)
            }
        }
        updateBgText()
        tvBg.setOnClickListener {
            bgIndex = (bgIndex + 1) % subtitleBgStyles.size
            playerPrefs.subtitleBgStyle = subtitleBgStyles[bgIndex]
            updateBgText()
            updateSubtitlePreview()
        }

        var pendingSubtitlePosition = playerPrefs.subtitlePosition
        sbPosition.progress = (pendingSubtitlePosition * 100).toInt()
        sbPosition.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) pendingSubtitlePosition = progress / 100f
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                playerPrefs.subtitlePosition = pendingSubtitlePosition
            }
        })

        val encodings = arrayOf("auto", "UTF-8", "GBK", "GB2312", "Big5", "Shift_JIS", "EUC-KR")
        var encIndex = encodings.indexOf(playerPrefs.subtitleEncoding).takeIf { it >= 0 } ?: 0
        fun updateEncText() { tvEncoding.text = if (encIndex == 0) getString(R.string.settings_encoding_auto) else encodings[encIndex] }
        updateEncText()
        tvEncoding.setOnClickListener {
            encIndex = (encIndex + 1) % encodings.size
            playerPrefs.subtitleEncoding = encodings[encIndex]
            updateEncText()
        }
    }
}
