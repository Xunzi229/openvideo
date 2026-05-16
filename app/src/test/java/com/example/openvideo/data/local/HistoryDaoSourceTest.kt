package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HistoryDaoSourceTest {

    @Test
    fun historyDaoReturnsLatestPlaybackFirst() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("""@Query("SELECT * FROM play_history ORDER BY timestamp DESC")"""))
    }

    private fun sourceFile(): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "data", "local", "HistoryDao.kt")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
