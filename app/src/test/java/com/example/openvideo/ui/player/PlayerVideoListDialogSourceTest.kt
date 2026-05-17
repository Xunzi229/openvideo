package com.example.openvideo.ui.player

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlayerVideoListDialogSourceTest {

    @Test
    fun recycledSessionThumbnailsAreClearedWithApplicationContext() {
        val source = String(Files.readAllBytes(dialogSource()))
        val recycleBlock = source.substringAfter("override fun onViewRecycled(holder: VH) {")
            .substringBefore("\n        override fun getItemCount()")

        assertTrue(recycleBlock.contains("Glide.with(holder.thumb.context.applicationContext).clear(holder.thumb)"))
        assertTrue(recycleBlock.contains("super.onViewRecycled(holder)"))
    }

    private fun dialogSource(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerVideoListDialog.kt"
        )
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
