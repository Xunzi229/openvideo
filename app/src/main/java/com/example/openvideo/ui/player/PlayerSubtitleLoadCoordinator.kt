package com.example.openvideo.ui.player

import android.net.Uri
import com.example.openvideo.core.subtitle.SubtitleItem
import com.example.openvideo.core.subtitle.SubtitleLoader

/**
 * 字幕加载 IO 与 [PlayerSubtitleLoadPolicy] 路由集中在此，Activity 只负责调度与 UI。
 */
object PlayerSubtitleLoadCoordinator {

    fun load(
        uriString: String,
        videoPath: String,
        loader: SubtitleLoader
    ): List<SubtitleItem> =
        when (val request = PlayerSubtitleLoadPolicy.resolve(uriString, videoPath)) {
            is PlayerSubtitleLoadRequest.SidecarFile -> {
                val subtitleFiles = loader.findSubtitleFiles(request.videoPath)
                if (subtitleFiles.isNotEmpty()) {
                    loader.loadFromFile(subtitleFiles[0])
                } else {
                    emptyList()
                }
            }
            is PlayerSubtitleLoadRequest.SubtitleUri ->
                loader.loadFromUri(Uri.parse(request.uriString))
            PlayerSubtitleLoadRequest.None -> emptyList()
        }
}
