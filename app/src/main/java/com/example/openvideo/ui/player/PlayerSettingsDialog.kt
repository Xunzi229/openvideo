package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.RadioGroup
import com.example.openvideo.R
import com.example.openvideo.core.player.AspectRatio
import com.example.openvideo.core.player.DecodeMode
import com.example.openvideo.core.player.PlayerManager
import com.example.openvideo.core.player.RenderMode
import com.google.android.material.switchmaterial.SwitchMaterial

class PlayerSettingsDialog(
    context: Context,
    private val playerManager: PlayerManager,
    private val viewModel: PlayerViewModel
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_player_settings)

        window?.apply {
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        setupDecodeMode()
        setupRenderMode()
        setupAspectRatio()
        setupDanmaku()
        setupBgAudio()
    }

    private fun setupDecodeMode() {
        val rg = findViewById<RadioGroup>(R.id.rg_decode)
        val rbSoft = findViewById<RadioButton>(R.id.rb_soft_decode)
        val rbHard = findViewById<RadioButton>(R.id.rb_hard_decode)

        when (playerManager.decodeMode) {
            DecodeMode.SOFT -> rbSoft.isChecked = true
            DecodeMode.HARD -> rbHard.isChecked = true
        }

        rg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_soft_decode -> viewModel.setDecodeMode(DecodeMode.SOFT)
                R.id.rb_hard_decode -> viewModel.setDecodeMode(DecodeMode.HARD)
            }
        }
    }

    private fun setupRenderMode() {
        val rg = findViewById<RadioGroup>(R.id.rg_render)
        val rbSurface = findViewById<RadioButton>(R.id.rb_surface)
        val rbTexture = findViewById<RadioButton>(R.id.rb_texture)

        when (playerManager.renderMode) {
            RenderMode.SURFACE -> rbSurface.isChecked = true
            RenderMode.TEXTURE -> rbTexture.isChecked = true
        }

        rg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_surface -> viewModel.setRenderMode(RenderMode.SURFACE)
                R.id.rb_texture -> viewModel.setRenderMode(RenderMode.TEXTURE)
            }
        }
    }

    private fun setupAspectRatio() {
        val rg = findViewById<RadioGroup>(R.id.rg_ratio)

        when (playerManager.aspectRatio) {
            AspectRatio.DEFAULT -> rg.check(R.id.rb_ratio_default)
            AspectRatio.RATIO_4_3 -> rg.check(R.id.rb_ratio_4_3)
            AspectRatio.RATIO_16_9 -> rg.check(R.id.rb_ratio_16_9)
            else -> rg.check(R.id.rb_ratio_default)
        }

        rg.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_ratio_default -> viewModel.setAspectRatio(AspectRatio.DEFAULT)
                R.id.rb_ratio_4_3 -> viewModel.setAspectRatio(AspectRatio.RATIO_4_3)
                R.id.rb_ratio_16_9 -> viewModel.setAspectRatio(AspectRatio.RATIO_16_9)
            }
        }
    }

    private fun setupDanmaku() {
        val switch = findViewById<SwitchMaterial>(R.id.switch_danmaku)
        switch.isChecked = false // Placeholder — danmaku not yet implemented
        switch.isEnabled = false
    }

    private fun setupBgAudio() {
        val switch = findViewById<SwitchMaterial>(R.id.switch_bg_audio)
        switch.isChecked = false
    }
}
