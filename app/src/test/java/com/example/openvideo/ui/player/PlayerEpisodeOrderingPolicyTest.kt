package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerEpisodeOrderingPolicyTest {

    @Test
    fun sortsSeasonEpisodeNamesByEpisodeNumber() {
        val ordered = PlayerEpisodeOrderingPolicy.orderCandidates(
            listOf(
                video(id = 10, title = "Modern Family S01E10.mp4"),
                video(id = 2, title = "Modern Family S01E02.mp4"),
                video(id = 1, title = "Modern Family S01E01.mp4")
            )
        )

        assertEquals(listOf(1L, 2L, 10L), ordered.map { it.id })
    }

    @Test
    fun sortsChineseEpisodeNamesByEpisodeNumber() {
        val ordered = PlayerEpisodeOrderingPolicy.orderCandidates(
            listOf(
                video(id = 12, title = "Show \u7b2c12\u96c6.mp4"),
                video(id = 3, title = "Show \u7b2c03\u96c6.mp4"),
                video(id = 2, title = "Show \u7b2c2\u96c6.mp4")
            )
        )

        assertEquals(listOf(2L, 3L, 12L), ordered.map { it.id })
    }

    @Test
    fun keepsFallbackDeterministicWhenEpisodeNumberIsMissing() {
        val ordered = PlayerEpisodeOrderingPolicy.orderCandidates(
            listOf(
                video(id = 3, title = "clip b.mp4", dateAdded = 30),
                video(id = 2, title = "clip a.mp4", dateAdded = 20),
                video(id = 1, title = "clip a.mp4", dateAdded = 10)
            )
        )

        assertEquals(listOf(1L, 2L, 3L), ordered.map { it.id })
    }

    private fun video(
        id: Long,
        title: String,
        dateAdded: Long = id
    ): PlayerEpisodeOrderingCandidate =
        PlayerEpisodeOrderingCandidate(
            id = id,
            title = title,
            path = "/storage/emulated/0/Shows/$title",
            dateAdded = dateAdded
        )
}
