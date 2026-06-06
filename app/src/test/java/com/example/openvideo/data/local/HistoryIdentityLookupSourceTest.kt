package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class HistoryIdentityLookupSourceTest {

    @Test
    fun historyDaoCanFindLatestHistoryByMediaIdentityId() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("suspend fun getByMediaIdentityId(mediaIdentityId: Long): HistoryEntity?"))
        assertTrue(source.contains("WHERE mediaIdentityId = :mediaIdentityId"))
        assertTrue(source.contains("ORDER BY timestamp DESC"))
        assertTrue(source.contains("LIMIT 1"))
    }

    private fun sourceFile(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "data",
            "local",
            "HistoryDao.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
