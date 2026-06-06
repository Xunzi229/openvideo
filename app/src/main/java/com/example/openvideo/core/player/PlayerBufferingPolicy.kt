package com.example.openvideo.core.player

data class BufferingProfile(
    val name: Name,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int
) {
    enum class Name {
        LOCAL_FILE,
        NETWORK_PROGRESSIVE,
        ADAPTIVE_STREAM,
        RTSP_STREAM
    }

}

object PlayerBufferingPolicy {
    fun profileFor(uriString: String): BufferingProfile {
        val normalized = uriString.lowercase()
        val normalizedPath = normalized.substringBefore('?').substringBefore('#')
        return when {
            normalizedPath.endsWith(".m3u8") || normalizedPath.endsWith(".mpd") ->
                BufferingProfile(
                    name = BufferingProfile.Name.ADAPTIVE_STREAM,
                    minBufferMs = 20_000,
                    maxBufferMs = 60_000,
                    bufferForPlaybackMs = 2_000,
                    bufferForPlaybackAfterRebufferMs = 4_000
                )
            normalized.startsWith("rtsp://") ->
                BufferingProfile(
                    name = BufferingProfile.Name.RTSP_STREAM,
                    minBufferMs = 5_000,
                    maxBufferMs = 30_000,
                    bufferForPlaybackMs = 1_000,
                    bufferForPlaybackAfterRebufferMs = 2_000
                )
            normalized.startsWith("http://") || normalized.startsWith("https://") ->
                BufferingProfile(
                    name = BufferingProfile.Name.NETWORK_PROGRESSIVE,
                    minBufferMs = 15_000,
                    maxBufferMs = 50_000,
                    bufferForPlaybackMs = 1_500,
                    bufferForPlaybackAfterRebufferMs = 3_000
                )
            else ->
                BufferingProfile(
                    name = BufferingProfile.Name.LOCAL_FILE,
                    minBufferMs = 2_000,
                    maxBufferMs = 20_000,
                    bufferForPlaybackMs = 250,
                    bufferForPlaybackAfterRebufferMs = 500
                )
        }
    }
}
