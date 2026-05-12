package com.example.openvideo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSettingsSpecTest {

    @Test
    fun actionSpecKeepsTitleValueAndAction() {
        var clicked = false
        val spec = PlayerSettingsActionSpec(
            title = "Double tap",
            value = "Forward"
        ) {
            clicked = true
        }

        assertEquals("Double tap", spec.title)
        assertEquals("Forward", spec.value)
        spec.onClick()
        assertEquals(true, clicked)
    }

    @Test
    fun actionSpecSupportsRowsWithoutValue() {
        val spec = PlayerSettingsActionSpec(title = "Reset") {}

        assertEquals("Reset", spec.title)
        assertEquals(null, spec.value)
    }

    @Test
    fun switchSpecKeepsTitleCheckedAndAction() {
        var changedTo: Boolean? = null
        val spec = PlayerSettingsSwitchSpec(
            title = "Background playback",
            checked = false
        ) { checked ->
            changedTo = checked
        }

        assertEquals("Background playback", spec.title)
        assertEquals(false, spec.checked)
        spec.onChanged(true)
        assertEquals(true, changedTo)
    }
}
