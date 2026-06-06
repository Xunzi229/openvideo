package com.example.openvideo.core.subtitle

object AssParser {

    fun parse(content: String): List<SubtitleItem> {
        val items = mutableListOf<SubtitleItem>()
        val lines = content.split("\r\n", "\n")
        val styles = mutableMapOf<String, SubtitleCueStyle>()
        var inEvents = false
        var inStyles = false
        var styleFormat = DEFAULT_STYLE_FORMAT
        var eventFormat = DEFAULT_EVENT_FORMAT
        var index = 0

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[V4+ Styles]", ignoreCase = true) -> {
                    inStyles = true
                    inEvents = false
                }
                trimmed.startsWith("[Events]", ignoreCase = true) -> {
                    inEvents = true
                    inStyles = false
                }
                trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                    inEvents = false
                    inStyles = false
                }
                inStyles && trimmed.startsWith("Format:", ignoreCase = true) -> {
                    styleFormat = parseFormat(trimmed.substringAfter(":"))
                }
                inStyles && trimmed.startsWith("Style:", ignoreCase = true) -> {
                    parseStyle(trimmed, styleFormat)?.let { (name, style) ->
                        styles[name.normalizedAssKey()] = style
                    }
                }
                inEvents && trimmed.startsWith("Format:", ignoreCase = true) -> {
                    eventFormat = parseFormat(trimmed.substringAfter(":"))
                }
                inEvents && trimmed.startsWith("Dialogue:") -> {
                    parseDialogue(trimmed, eventFormat, styles)?.let { items.add(it.copy(index = index++)) }
                }
            }
        }
        return items
    }

    private fun parseStyle(
        line: String,
        format: List<String>
    ): Pair<String, SubtitleCueStyle>? {
        val parts = line.substringAfter("Style:").split(",", limit = format.size)
        val fields = fieldsByName(format, parts)
        val name = fields["name"]?.trim().orEmpty()
        if (name.isEmpty()) return null

        return name to SubtitleCueStyle(
            fontName = fields["fontname"]?.trim()?.takeIf { it.isNotEmpty() },
            fontSizeSp = fields["fontsize"]?.trim()?.toFloatOrNull(),
            primaryColor = parseAssColor(fields["primarycolour"]),
            outlineColor = parseAssColor(fields["outlinecolour"]),
            outlineWidth = fields["outline"]?.trim()?.toFloatOrNull(),
            shadowDepth = fields["shadow"]?.trim()?.toFloatOrNull(),
            alignment = fields["alignment"]?.trim()?.toIntOrNull(),
            marginL = fields["marginl"]?.trim()?.toIntOrNull(),
            marginR = fields["marginr"]?.trim()?.toIntOrNull(),
            marginV = fields["marginv"]?.trim()?.toIntOrNull()
        )
    }

    private fun parseDialogue(
        line: String,
        format: List<String>,
        styles: Map<String, SubtitleCueStyle>
    ): SubtitleItem? {
        val parts = line.substringAfter("Dialogue:").split(",", limit = format.size)
        val fields = fieldsByName(format, parts)
        val startTime = parseAssTime(fields["start"]?.trim().orEmpty())
        val endTime = parseAssTime(fields["end"]?.trim().orEmpty())
        val text = fields["text"].orEmpty()
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace(Regex("\\{[^}]*\\}"), "")
            .trim()
        val style = fields["style"]?.let { styles[it.normalizedAssKey()] }

        if (text.isEmpty()) return null
        return SubtitleItem(0, startTime, endTime, text, style)
    }

    private fun parseAssTime(time: String): Long {
        val parts = time.split(":", ".")
        if (parts.size != 4) return 0
        val hours = parts[0].toLongOrNull() ?: 0
        val minutes = parts[1].toLongOrNull() ?: 0
        val seconds = parts[2].toLongOrNull() ?: 0
        val centis = parts[3].toLongOrNull() ?: 0
        return hours * 3600000 + minutes * 60000 + seconds * 1000 + centis * 10
    }

    private fun parseFormat(value: String): List<String> =
        value.split(",").map { it.normalizedAssKey() }.filter { it.isNotEmpty() }

    private fun fieldsByName(format: List<String>, parts: List<String>): Map<String, String> =
        buildMap {
            format.forEachIndexed { index, name ->
                if (index < parts.size) put(name, parts[index])
            }
        }

    private fun parseAssColor(value: String?): Int? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val hex = raw
            .removePrefix("&H")
            .removePrefix("&h")
            .removePrefix("H")
            .removePrefix("h")
            .removeSuffix("&")
        val padded = when (hex.length) {
            6 -> "00$hex"
            8 -> hex
            else -> return null
        }
        val assAlpha = padded.substring(0, 2).toIntOrNull(16) ?: return null
        val blue = padded.substring(2, 4).toIntOrNull(16) ?: return null
        val green = padded.substring(4, 6).toIntOrNull(16) ?: return null
        val red = padded.substring(6, 8).toIntOrNull(16) ?: return null
        val alpha = 255 - assAlpha
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun String.normalizedAssKey(): String = trim().lowercase()

    private val DEFAULT_STYLE_FORMAT = listOf(
        "name",
        "fontname",
        "fontsize",
        "primarycolour",
        "secondarycolour",
        "outlinecolour",
        "backcolour",
        "bold",
        "italic",
        "underline",
        "strikeout",
        "scalex",
        "scaley",
        "spacing",
        "angle",
        "borderstyle",
        "outline",
        "shadow",
        "alignment",
        "marginl",
        "marginr",
        "marginv",
        "encoding"
    )

    private val DEFAULT_EVENT_FORMAT = listOf(
        "layer",
        "start",
        "end",
        "style",
        "name",
        "marginl",
        "marginr",
        "marginv",
        "effect",
        "text"
    )
}
