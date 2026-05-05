package com.example.openvideo.core.subtitle

object SrtParser {

    fun parse(content: String): List<SubtitleItem> {
        val items = mutableListOf<SubtitleItem>()
        val blocks = content.trim().split("\r\n\r\n", "\n\n")

        for (block in blocks) {
            val lines = block.trim().split("\r\n", "\n")
            if (lines.size < 3) continue

            val index = lines[0].trim().toIntOrNull() ?: continue
            val timeParts = lines[1].split("-->")
            if (timeParts.size != 2) continue

            val startTime = parseTime(timeParts[0].trim())
            val endTime = parseTime(timeParts[1].trim())
            val text = lines.subList(2, lines.size).joinToString("\n")

            items.add(SubtitleItem(index, startTime, endTime, text))
        }
        return items
    }

    private fun parseTime(time: String): Long {
        val parts = time.split(":", ",")
        if (parts.size != 4) return 0
        val hours = parts[0].toLongOrNull() ?: 0
        val minutes = parts[1].toLongOrNull() ?: 0
        val seconds = parts[2].toLongOrNull() ?: 0
        val millis = parts[3].toLongOrNull() ?: 0
        return hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
    }
}
