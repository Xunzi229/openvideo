package com.example.openvideo.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DeferredFeaturePolicySourceTest {

    @Test
    fun unimplementedUserEntriesStayHiddenBehindPolicyFlags() {
        val policy = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "core", "ui",
            "DeferredFeaturePolicy.kt"
        ).readText()
        val sheet = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerSubtitleSettingsSheet.kt"
        ).readText()
        val binder = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "player",
            "PlayerControlsBinder.kt"
        ).readText()
        val sources = rootFile(
            "app", "src", "main", "java", "com", "example", "openvideo", "ui", "sources",
            "SourcesFragment.kt"
        ).readText()
        val doc = rootFile("design", "rules", "deferred-features.md").readText()

        assertTrue(policy.contains("const val ONLINE_SUBTITLE_SEARCH_VISIBLE = false"))
        assertTrue(policy.contains("const val SOURCE_FUTURE_ADAPTERS_VISIBLE = false"))
        assertTrue(policy.contains("const val PLAYER_CAST_VISIBLE = false"))
        assertTrue(sheet.contains("DeferredFeaturePolicy.ONLINE_SUBTITLE_SEARCH_VISIBLE"))
        assertTrue(binder.contains("DeferredFeaturePolicy.PLAYER_CAST_VISIBLE"))
        assertTrue(sources.contains("DeferredFeaturePolicy.SOURCE_FUTURE_ADAPTERS_VISIBLE"))
        assertTrue(doc.contains("未实现功能：先隐藏，预留后续开发"))
        assertTrue(doc.contains("OnlineSubtitleClient"))
        assertFalse(policy.contains("ONLINE_SUBTITLE_SEARCH_VISIBLE = true"))
        assertFalse(policy.contains("SOURCE_FUTURE_ADAPTERS_VISIBLE = true"))
        assertFalse(policy.contains("PLAYER_CAST_VISIBLE = true"))
    }

    private fun Path.readText(): String = String(Files.readAllBytes(this))

    private fun rootFile(vararg parts: String): Path =
        sequenceOf(
            parts.fold(Paths.get("")) { path, part -> path.resolve(part) },
            parts.fold(Paths.get("..")) { path, part -> path.resolve(part) }
        ).first(Files::exists)
}
