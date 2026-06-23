package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerHistorySourceTest {

    @Test
    fun activityPassesLocalPathToPlayerViewModel() {
        val source = String(Files.readAllBytes(sourceFile("PlayerActivity.kt")))
        val onCreate = source.substringAfter("override fun onCreate(savedInstanceState: Bundle?) {")
            .substringBefore("\n    private fun applyPlayerSettings()")

        assertTrue(
            "PlayerActivity should sanitize the playback URI before initializing the player",
            onCreate.contains("LocalMediaUriPolicy.playbackUri(uriString)")
        )
        assertTrue(
            "PlayerActivity should still pass video_path into PlayerViewModel so local playback history keeps a playable local path",
            onCreate.contains("viewModel.initialize(LocalMediaUriPolicy.playbackUri(uriString), title, id, videoPath)")
        )
    }

    @Test
    fun playerReadyImmediatelySavesHistoryForRecentPlayback() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))
        val readyBlock = source.substringAfter("if (playbackState == androidx.media3.common.Player.STATE_READY) {")
            .substringBefore("\n                }")

        assertTrue(
            "PlayerViewModel should save history when media becomes ready so local-menu playback appears in Recent immediately",
            readyBlock.contains("markPlaybackStarted()")
        )
    }

    @Test
    fun playbackStartUpdatesRecentWithoutOverwritingSavedProgress() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))

        assertTrue(
            "Playback start should use a dedicated history update for Recent instead of saveHistory with currentPosition=0",
            source.contains("private fun markPlaybackStarted()")
        )
        assertTrue(
            "Playback start should preserve old resume progress when a history row already exists",
            source.contains("history?.lastPosition ?: 0L")
        )
    }

    @Test
    fun viewModelRegistersPlayerListenerBeforePreparingMedia() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))
        val initialize = source.substringAfter("fun initialize(uri: Uri, title: String, id: Long, path: String = \"\") {")
            .substringBefore("\n    fun restorePosition")

        assertTrue(
            "PlayerViewModel should add its listener before setMediaUri so fast local files cannot miss STATE_READY history saving",
            initialize.indexOf("playerManager.addListener(playerListener!!)") < initialize.indexOf("playerManager.setMediaUri(uri)")
        )
    }

    @Test
    fun saveHistoryUsesStoredVideoPathInsteadOfContentUriWhenAvailable() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))

        assertTrue(
            "PlayerViewModel should store the original video path when provided by local folders",
            source.contains("private var videoPath: String = \"\"")
        )
        assertTrue(
            "saveHistory should prefer stored videoPath over uri.toString()",
            source.contains("path = videoPath.ifBlank { uri.toString() }")
        )
        assertTrue(
            "Per-video playback memory should persist the current playback speed.",
            source.contains("speed = playerPrefs.speed")
        )
        assertTrue(
            "Per-video playback memory should persist the current aspect ratio key.",
            source.contains("aspectRatioKey = playerPrefs.aspectRatio.key")
        )
        assertTrue(
            "Per-video playback memory should persist the current content frame key.",
            source.contains("contentFrameKey = playerPrefs.contentFrameMode.key")
        )
        assertTrue(
            "Per-video playback memory should persist the current external subtitle URI.",
            source.contains("externalSubtitleUri = playerPrefs.externalSubtitleUri")
        )
        assertTrue(
            "Per-video playback memory should persist subtitle enabled state.",
            source.contains("subtitlesEnabled = playerPrefs.subtitlesEnabled")
        )
        assertTrue(
            "Per-video playback memory should persist audio mute state.",
            source.contains("audioMuted = playerPrefs.audioMuted")
        )
        assertTrue(
            "Per-video playback memory should persist the currently selected audio track group index.",
            source.contains("audioTrackGroupIndex = selectedAudioTrack?.groupIndex ?: -1")
        )
        assertTrue(
            "Per-video playback memory should persist the currently selected audio track index.",
            source.contains("audioTrackIndex = selectedAudioTrack?.trackIndex ?: -1")
        )
    }

    @Test
    fun activityRestoresPerVideoPlaybackMemoryAlongsideResumePosition() {
        val source = String(Files.readAllBytes(sourceFile("PlayerActivity.kt")))
        val onCreate = source.substringAfter("override fun onCreate(savedInstanceState: Bundle?) {")
            .substringBefore("\n    private fun applyPlayerSettings()")

        assertTrue(onCreate.contains("viewModel.restorePlaybackPreferences(id)"))
        assertTrue(onCreate.contains("viewModel.restorePosition(id, explicitStartPositionMs)"))
    }

    @Test
    fun viewModelAppliesStoredSpeedAndAspectRatioFromHistory() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))

        assertTrue(source.contains("fun restorePlaybackPreferences(videoId: Long)"))
        assertTrue(source.contains("playerPrefs.speed = history.speed"))
        assertTrue(source.contains("playerPrefs.aspectRatio = AspectRatio.fromKey(history.aspectRatioKey)"))
        assertTrue(source.contains("playerPrefs.contentFrameMode = ContentFrameMode.fromKey(history.contentFrameKey)"))
        assertTrue(source.contains("playerPrefs.externalSubtitleUri = history.externalSubtitleUri"))
        assertTrue(source.contains("playerPrefs.subtitlesEnabled = history.subtitlesEnabled"))
        assertTrue(source.contains("playerPrefs.audioMuted = history.audioMuted"))
        assertTrue(source.contains("pendingAudioSelection = PendingAudioSelection("))
        assertTrue(source.contains("_uiState.value = _uiState.value.copy("))
    }

    @Test
    fun viewModelIgnoresStalePlaybackMemoryAfterVideoSwitch() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))
        val restorePrefs = source.substringAfter("fun restorePlaybackPreferences(videoId: Long, onRestored: () -> Unit) {")
            .substringBefore("\n    fun togglePlayPause")
        val restorePosition = source.substringAfter("fun restorePosition(videoId: Long, fallbackPositionMs: Long = 0L) {")
            .substringBefore("\n    fun setSessionQueue")
        val switchToVideo = source.substringAfter("fun switchToVideo(")
            .substringBefore("\n    private fun markPlaybackStarted()")

        assertTrue(restorePrefs.contains("if (videoId != this@PlayerViewModel.videoId) return@launch"))
        assertTrue(restorePosition.contains("if (videoId != this@PlayerViewModel.videoId) return@launch"))
        assertTrue(switchToVideo.contains("if (item.id == videoId)"))
    }

    @Test
    fun activityWaitsForPlaybackMemoryRestoreBeforeApplyingDisplayAndSubtitleState() {
        val source = String(Files.readAllBytes(sourceFile("PlayerActivity.kt")))
        val onCreate = source.substringAfter("override fun onCreate(savedInstanceState: Bundle?) {")
            .substringBefore("\n    private fun applyPlayerSettings()")

        assertTrue(onCreate.contains("viewModel.restorePlaybackPreferences(id) {"))
        assertTrue(onCreate.contains("applyPlayerSettings()"))
        assertTrue(onCreate.contains("loadSubtitlesAsync("))
        assertTrue(onCreate.contains("playerPrefs.externalSubtitleUri.ifBlank { uriString }"))
    }

    @Test
    fun playerReadyAppliesPendingPerVideoAudioSelection() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))
        val readyBlock = source.substringAfter("if (playbackState == androidx.media3.common.Player.STATE_READY) {")
            .substringBefore("\n                }")

        assertTrue(readyBlock.contains("applyPendingAudioSelection()"))
    }

    @Test
    fun completedPlaybackIsPersistedAsStartPosition() {
        val source = String(Files.readAllBytes(sourceFile("PlayerViewModel.kt")))
        val persistablePosition = source
            .substringAfter("private fun currentPersistablePosition(): Long")
            .substringBefore("\n    private fun currentHistoryVideoItem")
        val saveHistory = source
            .substringAfter("private suspend fun persistCurrentPlaybackProgress()")
            .substringBefore("\n    private fun currentPersistablePosition")

        assertTrue(
            "Completed videos should save resume progress as 0 so reopening the same video starts from the beginning",
            persistablePosition.contains("playerManager.playbackState == Player.STATE_ENDED") &&
                persistablePosition.contains("0L")
        )
        assertTrue(
            "All normal history saves should use the completed-playback-aware position helper",
            saveHistory.contains("currentPersistablePosition()")
        )
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            name
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
