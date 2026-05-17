package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * JVM unit tests cannot construct [android.util.Rational]; source tests guard PiP conversion wiring.
 */
class PlayerPipPolicySourceTest {

    @Test
    fun aspectRatioDeclaresToRationalOnDataClass() {
        val source = policySource()
        assertTrue(source.contains("fun toRational(): Rational = Rational(numerator, denominator)"))
        assertTrue(source.contains("fun fallbackRational(): Rational = FALLBACK_ASPECT_RATIO.toRational()"))
    }

    @Test
    fun fallbackAspectRatioIsSixteenByNine() {
        val source = policySource()
        assertTrue(source.contains("FALLBACK_ASPECT_RATIO = PlayerPipAspectRatio(16, 9)"))
    }

    private fun policySource(): String {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerPipPolicy.kt"
        )
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
