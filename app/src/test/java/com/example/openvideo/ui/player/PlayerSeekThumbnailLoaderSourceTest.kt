package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerSeekThumbnailLoaderSourceTest {

    @Test
    fun loaderUsesMemoryCacheBeforeExtractingFrames() {
        val source = sourceText("PlayerSeekThumbnailLoader.kt")

        assertTrue(source.contains("PlayerSeekThumbnailPolicy.thumbnailCacheKey(videoUri.toString(), positionMs)"))
        assertTrue(source.contains("PlayerSeekThumbnailMemoryCache.get(cacheKey)?.let"))
        assertTrue(source.contains("PlayerSeekThumbnailMemoryCache.put(cacheKey, previewBitmap)"))
        assertTrue(
            source.indexOf("PlayerSeekThumbnailMemoryCache.get(cacheKey)?.let") <
                source.indexOf("MediaMetadataRetriever()")
        )
    }

    @Test
    fun loaderScalesExtractedFramesBeforeCaching() {
        val source = sourceText("PlayerSeekThumbnailLoader.kt")

        assertTrue(source.contains("private fun scaleForPreview(bitmap: Bitmap): Bitmap"))
        assertTrue(source.contains("PlayerSeekThumbnailPolicy.scaledThumbnailSize(bitmap.width, bitmap.height)"))
        assertTrue(source.contains("Bitmap.createScaledBitmap(bitmap, target.first, target.second, true)"))
        assertTrue(source.contains("val previewBitmap = bitmap?.let(::scaleForPreview)"))
        assertTrue(source.contains("PlayerSeekThumbnailMemoryCache.put(cacheKey, previewBitmap)"))
        assertTrue(source.contains("onLoaded(previewBitmap)"))
    }

    private fun sourceText(name: String): String = String(Files.readAllBytes(sourceFile(name)))

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "java", "com", "example", "openvideo", "ui", "player", name)
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
