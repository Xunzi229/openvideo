package com.example.openvideo.data.scanner

/**
 * 将 MediaStore 原始宽高 + ORIENTATION 转为用户可见的显示尺寸。
 * 手机竖拍视频常存为 1920×1080 且 orientation=90，直接用于方向判断会误判为横屏。
 */
object MediaStoreVideoDimensionsPolicy {

    fun displayDimensions(
        width: Int,
        height: Int,
        orientationDegrees: Int
    ): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return width to height
        val rotation = normalizedRotation(orientationDegrees)
        return if (rotation % 180 == 90) {
            height to width
        } else {
            width to height
        }
    }

    private fun normalizedRotation(orientationDegrees: Int): Int =
        ((orientationDegrees % 360) + 360) % 360
}
