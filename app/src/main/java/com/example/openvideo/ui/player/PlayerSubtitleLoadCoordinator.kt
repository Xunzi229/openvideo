package com.example.openvideo.ui.player

import android.net.Uri
import com.example.openvideo.core.subtitle.SubtitleCandidateSelection
import com.example.openvideo.core.subtitle.SubtitleCandidateSelectionPolicy
import com.example.openvideo.core.subtitle.SubtitleItem
import com.example.openvideo.core.subtitle.SubtitleLanguagePreference
import com.example.openvideo.core.subtitle.SubtitleLoader
import java.io.File

/**
 * 字幕加载 IO 与 [PlayerSubtitleLoadPolicy] 路由集中在此，Activity 只负责调度与 UI。
 */
object PlayerSubtitleLoadCoordinator {

    fun load(
        uriString: String,
        videoPath: String,
        loader: SubtitleLoader,
        requestHeaders: Map<String, String> = emptyMap(),
        rememberedSubtitlePath: String = "",
        languagePreference: SubtitleLanguagePreference = SubtitleLanguagePreference()
    ): List<SubtitleItem> =
        when (val request = PlayerSubtitleLoadPolicy.resolve(uriString, videoPath)) {
            is PlayerSubtitleLoadRequest.SidecarFile -> {
                val candidates = loader.findSubtitleCandidates(request.videoPath)
                when (val selection = SubtitleCandidateSelectionPolicy.select(
                    candidates = candidates,
                    rememberedPath = rememberedSubtitlePath,
                    languagePreference = languagePreference
                )) {
                    is SubtitleCandidateSelection.AutoApply ->
                        loader.loadFromFile(File(selection.candidate.path))
                    is SubtitleCandidateSelection.RequiresUserChoice,
                    SubtitleCandidateSelection.None -> emptyList()
                }
            }
            is PlayerSubtitleLoadRequest.SubtitleUri -> {
                val uri = Uri.parse(request.uriString)
                if (uri.scheme in setOf("http", "https")) {
                    loader.loadFromNetworkUrl(request.uriString, requestHeaders)
                } else {
                    loader.loadFromUri(uri)
                }
            }
            PlayerSubtitleLoadRequest.None -> emptyList()
        }
}
