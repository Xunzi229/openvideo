package com.openvideo.app.core.subtitle

import org.junit.Assert.*
import org.junit.Test

class SubtitleExportBoundaryTest {
    @Test fun exportPlanEqualityUsesByteContentsAndAllOutputProperties() {
        val plan = SubtitleUtf8ExportPolicy.planSrtCopy(listOf(SubtitleItem(1, 0, 1000, "hello")), "file.srt")
        val same = plan.copy(bytes = plan.bytes.copyOf())
        assertEquals(plan, plan)
        assertEquals(plan, same)
        assertEquals(plan.hashCode(), same.hashCode())
        assertNotEquals(plan, null)
        assertNotEquals(plan, "not a plan")
        val changes = listOf(plan.copy(content = "other"), plan.copy(bytes = byteArrayOf(1)),
            plan.copy(suggestedCopyName = "other.srt"), plan.copy(charsetName = "GBK"),
            plan.copy(lineCount = 2), plan.copy(overwritesOriginal = true))
        changes.forEach { assertNotEquals(plan, it) }
    }

    @Test fun exportSortsTiedCuesAndClampsNegativeTimes() {
        val items = listOf(SubtitleItem(3, 1000, 3000, "last"), SubtitleItem(2, 1000, 2000, "middle"), SubtitleItem(1, -1, 500, "first"))
        val plan = SubtitleUtf8ExportPolicy.planSrtCopy(items, "C:\\dir\\clip.srt")
        val parsed = SrtParser.parse(plan.content)
        assertEquals(listOf("first", "middle", "last"), parsed.map { it.text })
        assertEquals(0L, parsed.first().startTimeMs)
        assertEquals("clip.utf8.srt", plan.suggestedCopyName)
        assertEquals("subtitle.utf8.srt", SubtitleUtf8ExportPolicy.planSrtCopy(emptyList(), ".hidden").suggestedCopyName)
    }

    @Test fun originalTargetComparisonHandlesLocalAndOpaquePaths() {
        val cases = listOf(Triple("", "/a", false), Triple("/a", "", false),
            Triple("relative", "/a", false), Triple("/a", "content://sub/a", false),
            Triple("/ab", "/ac", false), Triple("/a", "/a/", false),
            Triple("file://C:/dir/a.srt", "C:\\dir\\a.srt", true),
            Triple("/ab//c.srt", "/ab/c.srt", true), Triple("/ab/c.srt", "/other/c.srt", false))
        cases.forEach { (target, original, expected) -> assertEquals("$target -> $original", expected, SubtitleUtf8ExportPolicy.targetsOriginalSubtitle(target, original)) }
    }
}
