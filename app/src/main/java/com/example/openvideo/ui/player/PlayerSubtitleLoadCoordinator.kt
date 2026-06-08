package com.example.openvideo.ui.player

import android.net.Uri
import com.example.openvideo.core.subtitle.SubtitleCandidate
import com.example.openvideo.core.subtitle.SubtitleCandidateSelection
import com.example.openvideo.core.subtitle.SubtitleCandidateSelectionPolicy
import com.example.openvideo.core.subtitle.SubtitleItem
import com.example.openvideo.core.subtitle.SubtitleLanguagePreference
import com.example.openvideo.core.subtitle.SubtitleLoader
import java.io.File

sealed interface PlayerSubtitleLoadOutcome {
    data class Loaded(val subtitles: List<SubtitleItem>) : PlayerSubtitleLoadOutcome
    data class RequiresUserChoice(val candidates: List<SubtitleCandidate>) : PlayerSubtitleLoadOutcome
    data object None : PlayerSubtitleLoadOutcome
}

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
        when (val outcome = loadWithOutcome(
            uriString = uriString,
            videoPath = videoPath,
            loader = loader,
            requestHeaders = requestHeaders,
            rememberedSubtitlePath = rememberedSubtitlePath,
            languagePreference = languagePreference
        )) {
            is PlayerSubtitleLoadOutcome.Loaded -> outcome.subtitles
            is PlayerSubtitleLoadOutcome.RequiresUserChoice,
            PlayerSubtitleLoadOutcome.None -> emptyList()
        }

    fun loadWithOutcome(
        uriString: String,
        videoPath: String,
        loader: SubtitleLoader,
        requestHeaders: Map<String, String> = emptyMap(),
        rememberedSubtitlePath: String = "",
        languagePreference: SubtitleLanguagePreference = SubtitleLanguagePreference()
    ): PlayerSubtitleLoadOutcome =
        when (val request = PlayerSubtitleLoadPolicy.resolve(uriString, videoPath)) {
            is PlayerSubtitleLoadRequest.SidecarFile -> {
                val candidates = loader.findSubtitleCandidates(request.videoPath)
                when (val selection = SubtitleCandidateSelectionPolicy.select(
                    candidates = candidates,
                    rememberedPath = rememberedSubtitlePath,
                    languagePreference = languagePreference
                )) {
                    is SubtitleCandidateSelection.AutoApply ->
                        PlayerSubtitleLoadOutcome.Loaded(loader.loadFromFile(File(selection.candidate.path)))
                    is SubtitleCandidateSelection.RequiresUserChoice ->
                        PlayerSubtitleLoadOutcome.RequiresUserChoice(selection.candidates)
                    SubtitleCandidateSelection.None -> PlayerSubtitleLoadOutcome.None
                }
            }
            is PlayerSubtitleLoadRequest.SubtitleUri -> {
                val uri = Uri.parse(request.uriString)
                val subtitles = if (uri.scheme in setOf("http", "https")) {
                    loader.loadFromNetworkUrl(request.uriString, requestHeaders)
                } else {
                    loader.loadFromUri(uri)
                }
                PlayerSubtitleLoadOutcome.Loaded(subtitles)
            }
            PlayerSubtitleLoadRequest.None -> PlayerSubtitleLoadOutcome.None
        }
}
