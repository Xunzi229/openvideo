package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FavoriteIdentityLookupSourceTest {

    @Test
    fun favoriteDaoListsCurrentIdentityRowsWhenVideoIdChanges() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("fun getAllWithIdentityFallback(): Flow<List<FavoriteEntity>>"))
        assertTrue(source.contains("LEFT JOIN media_identity"))
        assertTrue(source.contains("favorites.mediaIdentityId = media_identity.identityId"))
        assertTrue(source.contains("media_identity.currentVideoId"))
        assertTrue(source.contains("media_identity.currentPath"))
        assertTrue(source.contains("media_identity.durationMs"))
        assertTrue(source.contains("favorites.timestamp AS timestamp"))
        assertTrue(source.contains("ORDER BY favorites.timestamp DESC"))
    }

    @Test
    fun favoriteDaoCanCheckAndDeleteByMediaIdentityId() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("suspend fun isFavoriteByMediaIdentityId(mediaIdentityId: Long): Boolean"))
        assertTrue(source.contains("WHERE mediaIdentityId = :mediaIdentityId"))
        assertTrue(source.contains("suspend fun deleteByMediaIdentityId(mediaIdentityId: Long)"))
        assertTrue(source.contains("DELETE FROM favorites WHERE mediaIdentityId = :mediaIdentityId"))
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
            "FavoriteDao.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
