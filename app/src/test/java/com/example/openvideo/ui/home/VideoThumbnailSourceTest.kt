package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoThumbnailSourceTest {

    @Test
    fun videoRowsUsePlaceholderFallbackAndErrorForThumbnails() {
        val source = String(Files.readAllBytes(videoGridAdapterSource()))
        val glideBlock = source.substringAfter("Glide.with(holder.thumbnail)")
            .substringBefore(".into(holder.thumbnail)")

        assertTrue(glideBlock.contains(".placeholder(R.drawable.ic_movie)"))
        assertTrue(glideBlock.contains(".fallback(R.drawable.ic_movie)"))
        assertTrue(glideBlock.contains(".error(R.drawable.ic_movie)"))
    }

    private fun videoGridAdapterSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "home",
            "VideoGridAdapter.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
