package com.example.openvideo.ui.player

enum class PlayerPlaybackTickKind {
    AB_LOOP,
    INTRO,
    OUTRO,
    CLIP_LOOP
}

data class PlayerPlaybackTickSeek(
    val positionMs: Long,
    val kind: PlayerPlaybackTickKind,
    val hasSkippedIntro: Boolean,
    val hasSkippedOutro: Boolean
)

object PlayerPlaybackTickPolicy {
    const val UI_TICK_INTERVAL_MS = 500L

    fun seekTarget(
        currentPositionMs: Long,
        durationMs: Long,
        abLoopState: PlayerAbLoopState,
        abLoopPointA: Long,
        abLoopPointB: Long,
        skipIntroOutro: Boolean,
        introSeconds: Int,
        outroSeconds: Int,
        hasSkippedIntro: Boolean,
        hasSkippedOutro: Boolean,
        clipLoopPreview: Boolean,
        clipStartMs: Long,
        clipEndMs: Long
    ): PlayerPlaybackTickSeek? {
        PlayerAbLoopPolicy.loopSeekTarget(
            state = abLoopState,
            pointA = abLoopPointA,
            pointB = abLoopPointB,
            currentPositionMs = currentPositionMs
        )?.let { target ->
            return PlayerPlaybackTickSeek(
                positionMs = target,
                kind = PlayerPlaybackTickKind.AB_LOOP,
                hasSkippedIntro = hasSkippedIntro,
                hasSkippedOutro = hasSkippedOutro
            )
        }

        IntroOutroSkipPolicy.skipTarget(
            enabled = skipIntroOutro,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            introSeconds = introSeconds,
            outroSeconds = outroSeconds,
            hasSkippedIntro = hasSkippedIntro,
            hasSkippedOutro = hasSkippedOutro
        )?.let { target ->
            return PlayerPlaybackTickSeek(
                positionMs = target.positionMs,
                kind = when (target.kind) {
                    IntroOutroSkipPolicy.Kind.INTRO -> PlayerPlaybackTickKind.INTRO
                    IntroOutroSkipPolicy.Kind.OUTRO -> PlayerPlaybackTickKind.OUTRO
                },
                hasSkippedIntro = hasSkippedIntro || target.kind == IntroOutroSkipPolicy.Kind.INTRO,
                hasSkippedOutro = hasSkippedOutro || target.kind == IntroOutroSkipPolicy.Kind.OUTRO
            )
        }

        if (clipLoopPreview && clipStartMs >= 0L && clipEndMs > clipStartMs && currentPositionMs >= clipEndMs) {
            return PlayerPlaybackTickSeek(
                positionMs = clipStartMs,
                kind = PlayerPlaybackTickKind.CLIP_LOOP,
                hasSkippedIntro = hasSkippedIntro,
                hasSkippedOutro = hasSkippedOutro
            )
        }

        return null
    }
}
