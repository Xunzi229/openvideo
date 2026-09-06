package com.example.openvideo.core.prefs

import com.example.openvideo.core.prefs.SettingsBackupJson.Value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsBackupJsonTest {
    @Test
    fun roundTripsNestedObjectsAndAllSupportedValueTypes() {
        val value = Value.Object(linkedMapOf(
            "text" to Value.Text("A \"quote\" and C:\\Movies"),
            "integer" to Value.Number("-12"),
            "decimal" to Value.Number("1.25"),
            "enabled" to Value.Boolean(true),
            "disabled" to Value.Boolean(false),
            "empty" to Value.Null,
            "nested" to Value.Object(emptyMap())
        ))
        assertEquals(value, SettingsBackupJson.parseObject(SettingsBackupJson.stringify(value)))
    }

    @Test
    fun parsesWhitespaceAndSupportedEscapes() {
        val parsed = SettingsBackupJson.parseObject(""" { "text" : "\"\\\/\b\f\n\r\t", "n" : -0.5 } """)
        assertEquals("\"\\/\b\u000C\n\r\t", SettingsBackupJson.stringOrNull(parsed, "text"))
        assertEquals(-0.5f, SettingsBackupJson.floatOrNull(parsed, "n"))
    }

    @Test
    fun typedAccessorsReturnValuesWithoutCoercingOtherJsonTypes() {
        val obj = SettingsBackupJson.parseObject("""{"text":"hello","empty":"","n":42,"decimal":1.5,"yes":true,"no":false,"null":null,"obj":{}}""")
        assertEquals("hello", SettingsBackupJson.stringOrNull(obj, "text"))
        assertEquals(42, SettingsBackupJson.intOrNull(obj, "n"))
        assertEquals(1.5f, SettingsBackupJson.floatOrNull(obj, "decimal"))
        assertEquals(true, SettingsBackupJson.booleanOrNull(obj, "yes"))
        assertEquals(false, SettingsBackupJson.booleanOrNull(obj, "no"))
        assertNull(SettingsBackupJson.stringOrNull(obj, "empty"))
        assertNull(SettingsBackupJson.intOrNull(obj, "decimal"))
        for (key in listOf("missing", "null", "obj")) {
            assertNull(SettingsBackupJson.stringOrNull(obj, key))
            assertNull(SettingsBackupJson.intOrNull(obj, key))
            assertNull(SettingsBackupJson.floatOrNull(obj, key))
            assertNull(SettingsBackupJson.booleanOrNull(obj, key))
        }
        for (key in listOf("obj", "missing", "null", "n")) {
            assertEquals(Value.Object(emptyMap()), SettingsBackupJson.objectOrEmpty(obj, key))
        }
    }

    @Test
    fun rejectsOutOfRangeIntegerWithoutThrowing() {
        val obj = SettingsBackupJson.parseObject("""{"n":2147483648}""")
        assertNull(SettingsBackupJson.intOrNull(obj, "n"))
    }

    @Test
    fun reportsMalformedObjectsLiteralsNumbersAndStrings() {
        val malformed = listOf(
            "", "[]", "{", "{\"a\"}", "{\"a\":}", "{\"a\":-}", "{\"a\":x}",
            "{\"a\":tru}", "{\"a\":fals}", "{\"a\":nul}", "{\"a\":1;}",
            "{\"a\":1,}", "{\"a\":\"unterminated", "{\"a\":\"\\", "{\"a\":\"\\q\"}"
        )
        malformed.forEach { json ->
            assertThrows(json, SettingsBackupJson.ParseException::class.java) { SettingsBackupJson.parseObject(json) }
        }
    }
}
