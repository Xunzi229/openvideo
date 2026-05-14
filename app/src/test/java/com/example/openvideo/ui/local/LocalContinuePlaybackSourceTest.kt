package com.example.openvideo.ui.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class LocalContinuePlaybackSourceTest {

    @Test
    fun localFolderPageHasContinuePlaybackFabInBottomRight() {
        val layout = String(Files.readAllBytes(layoutFile()))

        assertTrue(layout.contains("""android:id="@+id/fab_continue_playback""""))
        assertTrue(layout.contains("""app:layout_constraintBottom_toBottomOf="parent""""))
        assertTrue(layout.contains("""app:layout_constraintEnd_toEndOf="parent""""))
        assertTrue(layout.contains("""android:contentDescription="@string/local_continue_playback""""))
    }

    @Test
    fun localFolderFragmentObservesContinuePlaybackVideoAndOpensPlayer() {
        val source = String(Files.readAllBytes(sourceFile("LocalFolderFragment.kt")))

        assertTrue(source.contains("private var continuePlaybackVideo: VideoItem? = null"))
        assertTrue(source.contains("private var continuePlaybackPositionMs: Long = 0L"))
        assertTrue(source.contains("viewModel.continuePlaybackVideo.collect { video ->"))
        assertTrue(source.contains("viewModel.continuePlaybackPositionMs.collect { positionMs ->"))
        assertTrue(source.contains("continuePlaybackFab.visibility = if (video == null) View.GONE else View.VISIBLE"))
        assertTrue(source.contains("continuePlaybackFab.setOnClickListener"))
        assertTrue(source.contains("openPlayer(video)"))
        assertTrue(source.contains("putSessionQueue(localVideosSnapshot.ifEmpty { listOf(video) })"))
        assertTrue(source.contains("putExtra(PlayerActivity.EXTRA_START_POSITION_MS, continuePlaybackPositionMs)"))
    }

    @Test
    fun localFolderViewModelExposesOnlyValidContinuePlaybackCandidate() {
        val source = String(Files.readAllBytes(sourceFile("LocalFolderViewModel.kt")))

        assertTrue(source.contains("val continuePlaybackVideo"))
        assertTrue(source.contains("repository.getHistory()"))
        assertTrue(source.contains("LocalContinuePlaybackPolicy.latestPlayableVideoId"))
        assertTrue(source.contains("visibleVideos.firstOrNull"))
        assertTrue(source.contains("val continuePlaybackPositionMs"))
        assertTrue(source.contains("selectedHistory?.lastPosition ?: 0L"))
    }

    @Test
    fun playerActivityHonorsExplicitContinuePlaybackPosition() {
        val source = String(Files.readAllBytes(playerSourceFile("PlayerActivity.kt")))

        assertTrue(source.contains("const val EXTRA_START_POSITION_MS"))
        assertTrue(source.contains("getLongExtra(EXTRA_START_POSITION_MS, 0L)"))
        assertTrue(source.contains("viewModel.restorePosition(id, explicitStartPositionMs)"))
    }

    private fun layoutFile(): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", "fragment_local_folders.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "local", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun playerSourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
