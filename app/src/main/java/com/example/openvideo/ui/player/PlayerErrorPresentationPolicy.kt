package com.example.openvideo.ui.player

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import com.example.openvideo.R

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

    fun present(errorCode: Int): Presentation {
        return when {
            isUnsupportedContainerError(errorCode) -> Presentation(
                titleRes = R.string.player_error_title_format,
                descRes  = R.string.player_error_desc_format,
                actions  = listOf(
                    ErrorAction.COPY_DIAGNOSTICS,
                    ErrorAction.GO_BACK
                )
            )
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

    private fun isUnsupportedContainerError(errorCode: Int): Boolean =
        errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED

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
