package com.example.openvideo.ui.player

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.openvideo.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlayerMediaInfo(
    val containerMime: String? = null,
    val date: String? = null,
    val durationMs: Long? = null,
    val bitrate: Int? = null,
    val tracks: List<PlayerMediaTrack> = emptyList()
) {
    val hasDtsAudio: Boolean
        get() = tracks.any { it.type == PlayerMediaTrack.Type.AUDIO && it.isDtsAudio }
}

data class PlayerMediaTrack(
    val index: Int,
    val type: Type,
    val mime: String,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Float? = null,
    val sampleRate: Int? = null,
    val channelCount: Int? = null,
    val bitrate: Int? = null,
    val language: String? = null,
    val profile: Int? = null,
    val level: Int? = null
) {
    enum class Type { VIDEO, AUDIO, SUBTITLE, UNKNOWN }

    val isDtsAudio: Boolean
        get() {
            val normalized = mime.lowercase(Locale.US)
            return normalized == "audio/vnd.dts" ||
                normalized == "audio/vnd.dts.hd" ||
                normalized.contains("dts") ||
                normalized.contains("dca")
        }
}

object PlayerMediaInfoReader {

    fun read(context: Context, source: String): PlayerMediaInfo? {
        if (source.isBlank()) return null
        val metadata = readMetadata(context, source)
        val tracks = readTracks(context, source)
        if (metadata == null && tracks.isEmpty()) return null
        return PlayerMediaInfo(
            containerMime = metadata?.containerMime,
            date = metadata?.date,
            durationMs = metadata?.durationMs,
            bitrate = metadata?.bitrate,
            tracks = tracks
        )
    }

    private fun readMetadata(context: Context, source: String): PlayerMediaMetadata? {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSourceCompat(context, source)
            PlayerMediaMetadata(
                containerMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
                date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                    ?: fileDateLabel(source),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            )
        }.getOrNull().also {
            runCatching { retriever.release() }
        }
    }

    private fun readTracks(context: Context, source: String): List<PlayerMediaTrack> {
        val extractor = MediaExtractor()
        return runCatching {
            extractor.setDataSourceCompat(context, source)
            (0 until extractor.trackCount).mapNotNull { index ->
                extractor.getTrackFormat(index).toPlayerMediaTrack(index)
            }
        }.getOrDefault(emptyList()).also {
            extractor.release()
        }
    }

    private fun MediaMetadataRetriever.setDataSourceCompat(context: Context, source: String) {
        val uri = Uri.parse(source)
        when (uri.scheme?.lowercase(Locale.US)) {
            "content", "android.resource" -> setDataSource(context, uri)
            "file" -> setDataSource(uri.path ?: source)
            "http", "https" -> setDataSource(source, emptyMap())
            else -> setDataSource(source)
        }
    }

    private fun MediaExtractor.setDataSourceCompat(context: Context, source: String) {
        val uri = Uri.parse(source)
        when (uri.scheme?.lowercase(Locale.US)) {
            "content", "android.resource" -> setDataSource(context, uri, null)
            "file" -> setDataSource(uri.path ?: source)
            "http", "https" -> setDataSource(source, emptyMap())
            else -> setDataSource(source)
        }
    }

    private fun MediaFormat.toPlayerMediaTrack(index: Int): PlayerMediaTrack? {
        val mime = stringValue(MediaFormat.KEY_MIME) ?: return null
        val type = when {
            mime.startsWith("video/") -> PlayerMediaTrack.Type.VIDEO
            mime.startsWith("audio/") -> PlayerMediaTrack.Type.AUDIO
            mime.startsWith("text/") || mime.startsWith("application/") -> PlayerMediaTrack.Type.SUBTITLE
            else -> PlayerMediaTrack.Type.UNKNOWN
        }
        return PlayerMediaTrack(
            index = index,
            type = type,
            mime = mime,
            width = intValue(MediaFormat.KEY_WIDTH),
            height = intValue(MediaFormat.KEY_HEIGHT),
            frameRate = intValue(MediaFormat.KEY_FRAME_RATE)?.toFloat(),
            sampleRate = intValue(MediaFormat.KEY_SAMPLE_RATE),
            channelCount = intValue(MediaFormat.KEY_CHANNEL_COUNT),
            bitrate = intValue(MediaFormat.KEY_BIT_RATE),
            language = stringValue(MediaFormat.KEY_LANGUAGE),
            profile = intValue(MediaFormat.KEY_PROFILE),
            level = intValue(MediaFormat.KEY_LEVEL)
        )
    }

    private fun MediaFormat.stringValue(key: String): String? =
        if (containsKey(key)) runCatching { getString(key) }.getOrNull()?.takeIf { it.isNotBlank() } else null

    private fun MediaFormat.intValue(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

    private data class PlayerMediaMetadata(
        val containerMime: String?,
        val date: String?,
        val durationMs: Long?,
        val bitrate: Int?
    )
}

private fun fileDateLabel(source: String): String? {
    val uri = Uri.parse(source)
    val path = if (uri.scheme == "file") uri.path else source.takeIf { uri.scheme.isNullOrBlank() }
    val file = path?.let(::File)?.takeIf { it.exists() } ?: return null
    val modifiedAt = file.lastModified().takeIf { it > 0L } ?: return null
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(modifiedAt))
}

fun PlayerMediaInfo.mediaInfoRows(
    context: Context,
    formatDuration: (Long) -> String
): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    date?.takeIf { it.isNotBlank() }?.let {
        rows += context.getString(R.string.player_settings_info_date) to it
    }
    containerMime?.let {
        rows += context.getString(R.string.player_settings_info_container) to containerLabel(it)
    }
    durationMs?.let {
        rows += context.getString(R.string.player_settings_info_duration) to formatDuration(it)
    }
    bitrate?.let {
        rows += context.getString(R.string.player_settings_info_bitrate) to bitrateLabel(it)
    }
    tracks.forEach { track ->
        rows += track.mediaInfoRows(context)
    }
    if (hasDtsAudio) {
        rows += context.getString(R.string.player_settings_info_audio_compatibility) to
            context.getString(R.string.player_settings_info_audio_dts_unsupported)
    }
    return rows
}

private fun PlayerMediaTrack.mediaInfoRows(context: Context): List<Pair<String, String>> {
    val streamName = context.getString(R.string.player_settings_info_stream, index + 1)
    val rows = mutableListOf<Pair<String, String>>(
        streamName to typeLabel(context),
        context.getString(R.string.player_settings_info_codec) to codecLabel(mime)
    )
    if (type == PlayerMediaTrack.Type.VIDEO && width != null && height != null) {
        rows += context.getString(R.string.player_settings_info_resolution) to "${width}x$height"
    }
    frameRate?.let {
        rows += context.getString(R.string.player_settings_info_frame_rate) to frameRateLabel(it)
    }
    if (type == PlayerMediaTrack.Type.AUDIO) {
        sampleRate?.let {
            rows += context.getString(R.string.player_settings_info_sample_rate) to "$it Hz"
        }
        channelCount?.let {
            rows += context.getString(R.string.player_settings_info_channels) to channelLabel(it)
        }
    }
    bitrate?.let {
        rows += context.getString(R.string.player_settings_info_bitrate) to bitrateLabel(it)
    }
    language?.let {
        rows += context.getString(R.string.player_settings_info_language) to it
    }
    profile?.let { profileValue ->
        val profileLabel = level?.let { "$profileValue / $it" } ?: profileValue.toString()
        rows += context.getString(R.string.player_settings_info_profile) to profileLabel
    }
    return rows
}

private fun PlayerMediaTrack.typeLabel(context: Context): String = when (type) {
    PlayerMediaTrack.Type.VIDEO -> context.getString(R.string.player_settings_info_type_video)
    PlayerMediaTrack.Type.AUDIO -> context.getString(R.string.player_settings_info_type_audio)
    PlayerMediaTrack.Type.SUBTITLE -> context.getString(R.string.player_settings_info_type_subtitle)
    PlayerMediaTrack.Type.UNKNOWN -> context.getString(R.string.player_settings_info_type_unknown)
}

private fun codecLabel(mime: String): String = when (mime.lowercase(Locale.US)) {
    "video/hevc", "video/h265" -> "HEVC (High Efficiency Video Coding)"
    "video/avc", "video/h264" -> "AVC (H.264)"
    "video/x-vnd.on2.vp9" -> "VP9"
    "video/av01" -> "AV1"
    "audio/mp4a-latm" -> "AAC"
    "audio/ac3" -> "AC-3"
    "audio/eac3" -> "E-AC-3"
    "audio/vnd.dts" -> "DCA (DTS Coherent Acoustics)"
    "audio/vnd.dts.hd" -> "DTS-HD"
    else -> mime
}

private fun containerLabel(mime: String): String = when (mime.lowercase(Locale.US)) {
    "video/x-matroska", "audio/x-matroska", "application/x-matroska" -> "Matroska / WebM"
    "video/webm", "audio/webm" -> "WebM"
    "video/mp4", "audio/mp4", "application/mp4" -> "MPEG-4"
    else -> mime
}

private fun frameRateLabel(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString()
    else String.format(Locale.US, "%.5f", value).trimEnd('0').trimEnd('.')

private fun channelLabel(count: Int): String = when (count) {
    1 -> "Mono"
    2 -> "Stereo"
    6 -> "5.1"
    8 -> "7.1"
    else -> "$count ch"
}

private fun bitrateLabel(bitsPerSecond: Int): String {
    return if (bitsPerSecond >= 1_000_000) {
        "${String.format(Locale.US, "%.1f", bitsPerSecond / 1_000_000.0)} Mbits/sec"
    } else {
        "${bitsPerSecond / 1000} kbits/sec"
    }
}
