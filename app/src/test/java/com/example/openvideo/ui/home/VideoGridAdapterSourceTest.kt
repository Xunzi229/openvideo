package com.example.openvideo.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VideoGridAdapterSourceTest {

    @Test
    fun recycledThumbnailIsClearedWithApplicationContext() {
        val source = String(Files.readAllBytes(adapterSource()))
        val recycleBlock = source.substringAfter("override fun onViewRecycled(holder: ViewHolder) {")
            .substringBefore("\n    fun startMultiSelectMode()")

        assertTrue(recycleBlock.contains("holder.onLoadCleared()"))
        assertTrue(recycleBlock.contains("Glide.with(holder.thumbnail.context.applicationContext).clear(holder.thumbnail)"))
    }

    private fun adapterSource(): Path {
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
