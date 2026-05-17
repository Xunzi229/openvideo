package com.example.openvideo.core.player

import android.content.Context
import android.content.Intent

/**
 * 集中构造 [PlaybackService] 的 start / stop Intent，避免 Activity 侧重复拼装 action / extras
 * 以及与 service 实现脱节。
 */
object PlaybackServiceIntents {

    fun start(context: Context, title: String, isPlaying: Boolean): Intent =
        Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_START
            putExtra(PlaybackService.EXTRA_TITLE, title)
            putExtra(PlaybackService.EXTRA_IS_PLAYING, isPlaying)
        }

    fun stop(context: Context): Intent =
        Intent(context, PlaybackService::class.java)
}
