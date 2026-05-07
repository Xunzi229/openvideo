package com.example.openvideo.core.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerAutoplaySourceTest {

    @Test
    fun settingMediaUriRequestsAutoplayBeforePrepare() {
        val source = String(Files.readAllBytes(playerManagerSource()))
        val method = source.substringAfter("fun setMediaUri(uri: Uri) {")
            .substringBefore("\n    }")

        val autoplayIndex = method.indexOf("playWhenReady = true")
        val prepareIndex = method.indexOf("prepare()")

        assertTrue("setMediaUri should request autoplay", autoplayIndex >= 0)
        assertTrue(
            "setMediaUri should set playWhenReady before prepare",
            autoplayIndex < prepareIndex
        )
    }

    private fun playerManagerSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "core",
            "player",
            "PlayerManager.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
