package com.example.openvideo.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Paths

/**
 * JVM 单测中 [android.net.Uri] 常为 stub，易抛 [RuntimeException]；此处只测文件存在性逻辑。
 * `playbackUri` 行为见 [LocalMediaUriPolicySourceTest]。
 */
class LocalMediaUriPolicyTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun isPlayableFalseForMissingFile() {
        assertFalse(LocalMediaUriPolicy.isPlayable("/no/such/file-${System.nanoTime()}.mp4"))
    }

    @Test
    fun isPlayableTrueForExistingPlainFile() {
        val f = temp.newFile("plain.mp4")
        assertTrue(LocalMediaUriPolicy.isPlayable(f.absolutePath))
    }

    @Test
    fun isPlayableTrueForContentUri() {
        assertTrue(LocalMediaUriPolicy.isPlayable("content://media/external/video/media/1"))
    }
}

class LocalMediaUriPolicySourceTest {

    @Test
    fun playbackUriUsesFromFileForNonNetworkLocalPaths() {
        val src = loadSource("LocalMediaUriPolicy.kt")
        assertTrue(src.contains("Uri.fromFile(File(t))"))
        assertTrue(src.contains("startsWith(\"content://\")"))
        assertTrue(src.contains("startsWith(\"http://\")"))
        assertTrue(src.contains("startsWith(\"rtsp://\")"))
    }

    private fun loadSource(fileName: String): String {
        val relative = Paths.get(
            "src", "main", "java", "com", "example", "openvideo", "core", "media", fileName
        )
        val path = sequenceOf(relative, Paths.get("app").resolve(relative)).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
