package com.example.openvideo.core.subtitle

object VttParser {

    fun parse(content: String): List<SubtitleItem> {
        val items = mutableListOf<SubtitleItem>()
        val lines = content.trim().split("\r\n", "\n")
        var index = 0
        var i = 0

        // Skip WEBVTT header
        while (i < lines.size && !lines[i].contains("-->")) {
            i++
        }

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                val timeParts = line.split("-->")
                if (timeParts.size == 2) {
                    val startTime = parseTime(timeParts[0].trim())
                    val endTime = parseTime(timeParts[1].trim())
                    val textLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        textLines.add(lines[i].trim())
                        i++
                    }
                    if (textLines.isNotEmpty()) {
                        items.add(SubtitleItem(index++, startTime, endTime, textLines.joinToString("\n")))
                    }
                }
            } else {
                i++
            }
        }
        return items
    }

    private fun parseTime(time: String): Long {
        val parts = time.split(":", ".")
        if (parts.size < 3) return 0
        val hours = parts[0].toLongOrNull() ?: 0
        val minutes = parts[1].toLongOrNull() ?: 0
        val seconds = parts[2].toLongOrNull() ?: 0
        val millis = if (parts.size > 3) parts[3].toLongOrNull() ?: 0 else 0
        return hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
    }
}
