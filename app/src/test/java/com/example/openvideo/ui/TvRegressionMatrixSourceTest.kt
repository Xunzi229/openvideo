package com.example.openvideo.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TvRegressionMatrixSourceTest {

    @Test
    fun tvRegressionMatrixDocumentsRemoteFocusPlaybackAndSourceCoverage() {
        val doc = rootText("docs", "roadmap", "tv-regression-matrix.md")

        assertTrue(doc.contains("P5-TV-REGRESSION-001"))
        assertTrue(doc.contains("Android TV Regression Matrix"))
        assertTrue(doc.contains("Launch and TV mode"))
        assertTrue(doc.contains("D-pad focus"))
        assertTrue(doc.contains("Playback remote keys"))
        assertTrue(doc.contains("Subtitles and audio"))
        assertTrue(doc.contains("Sources"))
        assertTrue(doc.contains("Settings"))
        assertTrue(doc.contains("Known limits"))
        assertTrue(doc.contains("adb install -r app\\build\\outputs\\apk\\debug\\app-debug.apk"))
        assertTrue(doc.contains("adb shell am start -n com.example.openvideo/.ui.MainActivity"))
        assertTrue(doc.contains("adb logcat -d -v time AndroidRuntime:E *:S"))
        assertTrue(doc.contains("Latest adb smoke"))
        assertTrue(doc.contains("71615e08"))
        assertTrue(doc.contains("phone-class"))
        assertTrue(doc.contains("TV-only visual traversal: not run"))
    }

    @Test
    fun tvRegressionMatrixStaysAlignedWithImplementedTvAndRemoteCodePaths() {
        val doc = rootText("docs", "roadmap", "tv-regression-matrix.md")
        val manifest = rootText("app", "src", "main", "AndroidManifest.xml")
        val mainActivity = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "MainActivity.kt"
        )
        val tvHome = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "tv",
            "TvHomeFragment.kt"
        )
        val player = rootText(
            "app",
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerActivity.kt"
        )

        assertTrue(manifest.contains("android.intent.category.LEANBACK_LAUNCHER"))
        assertTrue(mainActivity.contains("MainActivityTvModePolicy"))
        assertTrue(mainActivity.contains("TvHomeFragment()"))
        assertTrue(tvHome.contains("tv_card_continue"))
        assertTrue(player.contains("runRemoteSeekAction(event)"))
        assertTrue(player.contains("KEYCODE_DPAD_CENTER"))
        assertTrue(player.contains("KEYCODE_MEDIA_PLAY_PAUSE"))

        assertTrue(doc.contains("LEANBACK_LAUNCHER"))
        assertTrue(doc.contains("MainActivityTvModePolicy"))
        assertTrue(doc.contains("TvHomeFragment"))
        assertTrue(doc.contains("runRemoteSeekAction(event)"))
        assertTrue(doc.contains("KEYCODE_DPAD_CENTER"))
        assertTrue(doc.contains("KEYCODE_MEDIA_PLAY_PAUSE"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).first(Files::exists)
            }
}
