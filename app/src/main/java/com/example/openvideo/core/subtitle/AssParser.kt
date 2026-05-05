package com.example.openvideo.core.subtitle

object AssParser {

    fun parse(content: String): List<SubtitleItem> {
        val items = mutableListOf<SubtitleItem>()
        val lines = content.split("\r\n", "\n")
        var inEvents = false
        var index = 0

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[Events]") -> inEvents = true
                trimmed.startsWith("[") && trimmed.endsWith("]") -> inEvents = false
                inEvents && trimmed.startsWith("Dialogue:") -> {
                    parseDialogue(trimmed)?.let { items.add(it.copy(index = index++)) }
                }
            }
        }
        return items
    }

    private fun parseDialogue(line: String): SubtitleItem? {
        val parts = line.substringAfter("Dialogue:").split(",", limit = 10)
        if (parts.size < 10) return null

        val startTime = parseAssTime(parts[1].trim())
        val endTime = parseAssTime(parts[2].trim())
        val text = parts[9]
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace(Regex("\\{[^}]*\\}"), "")
            .trim()

        if (text.isEmpty()) return null
        return SubtitleItem(0, startTime, endTime, text)
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
}
