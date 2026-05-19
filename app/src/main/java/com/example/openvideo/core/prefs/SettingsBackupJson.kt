package com.example.openvideo.core.prefs

/**
 * 轻量 JSON 读写，供 [SettingsBackupSchema] 在 JVM 单元测试与 Android 运行时共用。
 */
internal object SettingsBackupJson {

    sealed class Value {
        data class Object(val entries: Map<String, Value>) : Value()
        data class Text(val value: kotlin.String) : Value()
        data class Number(val raw: kotlin.String) : Value()
        data class Boolean(val value: kotlin.Boolean) : Value()
        data object Null : Value()
    }

    class ParseException(message: String) : Exception(message)

    fun stringify(value: Value): String = when (value) {
        is Value.Object -> value.entries.entries.joinToString(separator = ",", prefix = "{", postfix = "}") { (key, entry) ->
            "${quote(key)}:${stringify(entry)}"
        }
        is Value.Text -> quote(value.value)
        is Value.Number -> value.raw
        is Value.Boolean -> value.value.toString()
        Value.Null -> "null"
    }

    fun parseObject(json: String): Value.Object = Reader(json.trim()).readObject()

    fun stringOrNull(obj: Value.Object, key: String): String? =
        (obj.entries[key] as? Value.Text)?.value?.takeIf { it.isNotEmpty() }

    fun intOrNull(obj: Value.Object, key: String): Int? =
        (obj.entries[key] as? Value.Number)?.raw?.toIntOrNull()

    fun floatOrNull(obj: Value.Object, key: String): Float? =
        (obj.entries[key] as? Value.Number)?.raw?.toFloatOrNull()

    fun booleanOrNull(obj: Value.Object, key: String): Boolean? =
        (obj.entries[key] as? Value.Boolean)?.value

    fun objectOrEmpty(obj: Value.Object, key: String): Value.Object =
        obj.entries[key] as? Value.Object ?: Value.Object(emptyMap())

    private fun quote(raw: String): String = buildString {
        append('"')
        raw.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else -> append(char)
            }
        }
        append('"')
    }

    private class Reader(private val input: String) {
        private var index = 0

        fun readObject(): Value.Object {
            expect('{')
            skipWhitespace()
            if (peek() == '}') {
                index++
                return Value.Object(emptyMap())
            }
            val entries = linkedMapOf<String, Value>()
            while (true) {
                val key = readString()
                skipWhitespace()
                expect(':')
                entries[key] = readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> {
                        index++
                        skipWhitespace()
                    }
                    '}' -> {
                        index++
                        break
                    }
                    else -> throw ParseException("Expected ',' or '}' at $index")
                }
            }
            return Value.Object(entries)
        }

        private fun readValue(): Value {
            skipWhitespace()
            return when (peek()) {
                '"' -> Value.Text(readString())
                '{' -> readObject()
                't' -> {
                    expectLiteral("true")
                    Value.Boolean(true)
                }
                'f' -> {
                    expectLiteral("false")
                    Value.Boolean(false)
                }
                'n' -> {
                    expectLiteral("null")
                    Value.Null
                }
                else -> Value.Number(readNumber())
            }
        }

        private fun readString(): String {
            expect('"')
            val builder = StringBuilder()
            while (index < input.length) {
                when (val char = input[index++]) {
                    '"' -> return builder.toString()
                    '\\' -> {
                        if (index >= input.length) throw ParseException("Unterminated escape at $index")
                        builder.append(
                            when (val escaped = input[index++]) {
                                '"', '\\', '/' -> escaped
                                'b' -> '\b'
                                'f' -> '\u000C'
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                else -> throw ParseException("Invalid escape \\$escaped at $index")
                            }
                        )
                    }
                    else -> builder.append(char)
                }
            }
            throw ParseException("Unterminated string")
        }

        private fun readNumber(): String {
            val start = index
            if (peek() == '-') index++
            while (index < input.length && input[index].isDigit()) index++
            if (index < input.length && input[index] == '.') {
                index++
                while (index < input.length && input[index].isDigit()) index++
            }
            if (index == start || (start + 1 == index && input[start] == '-')) {
                throw ParseException("Invalid number at $index")
            }
            return input.substring(start, index)
        }

        private fun expectLiteral(literal: String) {
            if (!input.startsWith(literal, index)) {
                throw ParseException("Expected $literal at $index")
            }
            index += literal.length
        }

        private fun expect(char: Char) {
            skipWhitespace()
            if (index >= input.length || input[index] != char) {
                throw ParseException("Expected '$char' at $index")
            }
            index++
        }

        private fun peek(): Char {
            skipWhitespace()
            if (index >= input.length) throw ParseException("Unexpected end at $index")
            return input[index]
        }

        private fun skipWhitespace() {
            while (index < input.length && input[index].isWhitespace()) index++
        }
    }
}
