package com.example.openvideo.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class OnlineSubtitlePrivacyNoticeSourceTest {

    @Test
    fun subtitleSettingsSheetHasManualOnlineSearchEntryBehindPrivacyNotice() {
        val layout = readText("src", "main", "res", "layout", "activity_player_subtitle_settings.xml")
        val source = readText(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerSubtitleSettingsSheet.kt"
        )
        val clickBlock = source.substringAfter("btnOnlineSubtitleSearch.setOnClickListener")
            .substringBefore("btnExportSubtitle.setOnClickListener")

        assertTrue(layout.contains("""android:id="@+id/btn_online_subtitle_search""""))
        assertTrue(layout.contains("""@string/player_settings_online_subtitle_search"""))
        assertTrue(source.contains("showOnlineSubtitlePrivacyNotice"))
        assertTrue(source.contains("OnlineSubtitlePrivacyPolicy.firstUseNotice()"))
        assertTrue(source.contains("MaterialAlertDialogBuilder"))
        assertFalse(source.contains("OnlineSubtitleClient"))
        assertFalse(clickBlock.contains(".search("))
    }

    @Test
    fun onlineSubtitlePrivacyNoticeRequestsCancelDefaultFocusForRemoteUse() {
        val source = readText(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "player",
            "PlayerSubtitleSettingsSheet.kt"
        )
        val noticeBlock = source.substringAfter("private fun showOnlineSubtitlePrivacyNotice()")
            .substringBefore("\n    override fun onViewCreated")

        assertTrue(noticeBlock.contains("val dialog = MaterialAlertDialogBuilder(requireContext())"))
        assertTrue(noticeBlock.contains("setNegativeButton(R.string.action_cancel, null)"))
        assertTrue(noticeBlock.contains("getButton(android.app.AlertDialog.BUTTON_NEGATIVE)"))
        assertTrue(noticeBlock.contains("cancelButton.post"))
        assertTrue(noticeBlock.contains("cancelButton.requestFocus()"))
    }

    @Test
    fun onlineSubtitlePrivacyStringsAndRoadmapExplainManualPayload() {
        val strings = readText("src", "main", "res", "values", "strings.xml")
        val zhStrings = readText("src", "main", "res", "values-zh-rCN", "strings.xml")
        val roadmap = readText("docs", "roadmap", "phases", "phase-4-subtitles-content", "README.md")

        assertTrue(strings.contains("player_settings_online_subtitle_privacy_title"))
        assertTrue(strings.contains("title, season, episode, and language"))
        assertTrue(strings.contains("does not upload file hashes"))
        assertTrue(zhStrings.contains("player_settings_online_subtitle_privacy_title"))
        assertTrue(zhStrings.contains("标题、季、集和语言"))
        assertTrue(zhStrings.contains("不会上传文件 hash"))
        assertTrue(roadmap.contains("P4-ONLINE-003 在线字幕首次隐私提示入口：Done"))
        assertTrue(roadmap.contains("用户确认后才进入后续搜索流程"))
    }

    private fun readText(vararg segments: String): String {
        val relativePath = Paths.get(segments.first(), *segments.drop(1).toTypedArray())
        val path: Path = sequenceOf(
            relativePath,
            Paths.get("..").resolve(relativePath),
            Paths.get("app").resolve(relativePath)
        ).first(Files::exists)
        return String(Files.readAllBytes(path))
    }
}
