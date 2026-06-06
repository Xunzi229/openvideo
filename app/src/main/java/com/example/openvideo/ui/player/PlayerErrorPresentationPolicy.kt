package com.example.openvideo.ui.player

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import com.example.openvideo.R
import com.example.openvideo.core.network.NetworkErrorClassifier

/**
 * 纯函数策略：将 [PlaybackException] 映射为用户可读的错误展示模型。
 *
 * 不做 IO，不持有 Context，便于在 JVM 单测里全量覆盖。
 * 文案 StringRes 由调用方（PlayerActivity）通过 [getString] 解析。
 */
@OptIn(UnstableApi::class)
object PlayerErrorPresentationPolicy {

    /** 错误展示模型。 */
    data class Presentation(
        @param:StringRes val titleRes: Int,
        @param:StringRes val descRes: Int,
        val actions: List<ErrorAction>
    )

    /** 错误可执行操作。 */
    enum class ErrorAction {
        /** 重新 prepare 当前视频。 */
        RETRY,
        /** 关闭硬解，切软件解码后重试。 */
        SWITCH_SOFTWARE_DECODER,
        /** 把最新诊断日志内容复制到剪贴板。 */
        COPY_DIAGNOSTICS,
        /** 退出播放器回到列表。 */
        GO_BACK
    }

    fun present(errorCode: Int, cause: Throwable? = null): Presentation {
        networkPresentation(errorCode, cause)?.let { return it }
        return when {
            isDecoderError(errorCode) -> Presentation(
                titleRes = R.string.player_error_title_decode,
                descRes  = R.string.player_error_desc_decode,
                actions  = listOf(
                    ErrorAction.SWITCH_SOFTWARE_DECODER,
                    ErrorAction.RETRY,
                    ErrorAction.COPY_DIAGNOSTICS,
                    ErrorAction.GO_BACK
                )
            )
            isIoError(errorCode) -> Presentation(
                titleRes = R.string.player_error_title_io,
                descRes  = R.string.player_error_desc_io,
                actions  = listOf(
                    ErrorAction.RETRY,
                    ErrorAction.COPY_DIAGNOSTICS,
                    ErrorAction.GO_BACK
                )
            )
            else -> Presentation(
                titleRes = R.string.player_error_title_general,
                descRes  = R.string.player_error_desc_general,
                actions  = listOf(
                    ErrorAction.RETRY,
                    ErrorAction.COPY_DIAGNOSTICS,
                    ErrorAction.GO_BACK
                )
            )
        }
    }

    private fun networkPresentation(errorCode: Int, cause: Throwable?): Presentation? {
        val classification = NetworkErrorClassifier.classifyPlaybackError(errorCode, cause)
        if (classification.type == NetworkErrorClassifier.Type.NON_NETWORK) return null
        val descRes = when (classification.type) {
            NetworkErrorClassifier.Type.CONNECTION_FAILED -> R.string.player_error_desc_network_connection
            NetworkErrorClassifier.Type.DNS_FAILED -> R.string.player_error_desc_network_dns
            NetworkErrorClassifier.Type.TIMEOUT -> R.string.player_error_desc_network_timeout
            NetworkErrorClassifier.Type.BAD_HTTP_STATUS -> R.string.player_error_desc_network_http
            NetworkErrorClassifier.Type.CLEARTEXT_NOT_PERMITTED -> R.string.player_error_desc_network_cleartext
            NetworkErrorClassifier.Type.UNKNOWN_NETWORK -> R.string.player_error_desc_network_unknown
            NetworkErrorClassifier.Type.NON_NETWORK -> return null
        }
        val actions = buildList {
            if (classification.isRetryable) add(ErrorAction.RETRY)
            add(ErrorAction.COPY_DIAGNOSTICS)
            add(ErrorAction.GO_BACK)
        }
        return Presentation(
            titleRes = R.string.player_error_title_network,
            descRes = descRes,
            actions = actions
        )
    }

    /** 解码 / Codec 相关错误码。 */
    fun isDecoderError(errorCode: Int): Boolean = errorCode in setOf(
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED,
        PlaybackException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED
    )

    /** 文件 / 网络 IO 相关错误码。 */
    fun isIoError(errorCode: Int): Boolean = errorCode in setOf(
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE
    )
}
