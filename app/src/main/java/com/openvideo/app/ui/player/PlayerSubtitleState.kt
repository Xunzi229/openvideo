package com.openvideo.app.ui.player

import com.openvideo.app.core.subtitle.DualSubtitleState

internal fun PlayerUiState.withoutSubtitles(): PlayerUiState = copy(
    subtitles = emptyList(),
    dualSubtitles = DualSubtitleState(),
    currentSubtitle = ""
)
