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

    @Test
    fun videoListExplainsEpisodeOrderingBehavior() {
        val layout = String(Files.readAllBytes(layoutFile("dialog_player_video_list.xml")))
        val english = String(Files.readAllBytes(valuesFile("values", "strings.xml")))
        val chinese = String(Files.readAllBytes(valuesFile("values-zh-rCN", "strings.xml")))

        assertTrue(layout.contains("@+id/player_video_list_order_hint"))
        assertTrue(layout.contains("@string/player_video_list_order_hint"))
        assertTrue(english.contains("player_video_list_order_hint"))
        assertTrue(chinese.contains("player_video_list_order_hint"))
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

    private fun layoutFile(name: String): Path {
        val relativePath = Paths.get("src", "main", "res", "layout", name)
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }

    private fun valuesFile(folder: String, name: String): Path {
        val relativePath = Paths.get("src", "main", "res", folder, name)
        return sequenceOf(
            relativePath,
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
    }
}
