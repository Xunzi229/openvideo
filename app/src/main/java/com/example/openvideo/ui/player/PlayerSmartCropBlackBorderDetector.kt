package com.example.openvideo.ui.player

object PlayerSmartCropBlackBorderDetector {
    private const val MIN_BORDER_FRACTION = 0.08f
    private const val BLACK_LINE_RATIO = 0.92f
    private const val CONTENT_LINE_RATIO = 0.02f

    data class ContentBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    fun detect(
        width: Int,
        height: Int,
        isBlackPixel: (x: Int, y: Int) -> Boolean
    ): PlayerSmartCropBlackBorders {
        if (width <= 0 || height <= 0) {
            return PlayerSmartCropBlackBorders(left = false, top = false, right = false, bottom = false)
        }
        val minHorizontalBorder = (width * MIN_BORDER_FRACTION).toInt().coerceAtLeast(1)
        val minVerticalBorder = (height * MIN_BORDER_FRACTION).toInt().coerceAtLeast(1)

        return PlayerSmartCropBlackBorders(
            left = blackColumnsFromLeft(width, height, isBlackPixel) >= minHorizontalBorder,
            top = blackRowsFromTop(width, height, isBlackPixel) >= minVerticalBorder,
            right = blackColumnsFromRight(width, height, isBlackPixel) >= minHorizontalBorder,
            bottom = blackRowsFromBottom(width, height, isBlackPixel) >= minVerticalBorder
        )
    }

    fun detectFromContentRect(
        viewportWidth: Int,
        viewportHeight: Int,
        contentLeft: Int,
        contentTop: Int,
        contentRight: Int,
        contentBottom: Int
    ): PlayerSmartCropBlackBorders {
        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return PlayerSmartCropBlackBorders(left = false, top = false, right = false, bottom = false)
        }
        val minHorizontalBorder = (viewportWidth * MIN_BORDER_FRACTION).toInt().coerceAtLeast(1)
        val minVerticalBorder = (viewportHeight * MIN_BORDER_FRACTION).toInt().coerceAtLeast(1)

        return PlayerSmartCropBlackBorders(
            left = contentLeft.coerceAtLeast(0) >= minHorizontalBorder,
            top = contentTop.coerceAtLeast(0) >= minVerticalBorder,
            right = (viewportWidth - contentRight).coerceAtLeast(0) >= minHorizontalBorder,
            bottom = (viewportHeight - contentBottom).coerceAtLeast(0) >= minVerticalBorder
        )
    }

    fun detectContentBounds(
        width: Int,
        height: Int,
        isBlackPixel: (x: Int, y: Int) -> Boolean
    ): ContentBounds? {
        if (width <= 0 || height <= 0) return null

        val top = firstContentRow(width, height, isBlackPixel) ?: return null
        val bottom = lastContentRow(width, height, isBlackPixel) ?: return null
        val left = firstContentColumn(width, height, isBlackPixel) ?: return null
        val right = lastContentColumn(width, height, isBlackPixel) ?: return null

        if (right <= left || bottom <= top) return null
        return ContentBounds(left = left, top = top, right = right, bottom = bottom)
    }

    private fun blackRowsFromTop(width: Int, height: Int, isBlackPixel: (Int, Int) -> Boolean): Int {
        var rows = 0
        for (y in 0 until height) {
            if (!isBlackRow(width, y, isBlackPixel)) break
            rows++
        }
        return rows
    }

    private fun blackRowsFromBottom(width: Int, height: Int, isBlackPixel: (Int, Int) -> Boolean): Int {
        var rows = 0
        for (y in height - 1 downTo 0) {
            if (!isBlackRow(width, y, isBlackPixel)) break
            rows++
        }
        return rows
    }

    private fun blackColumnsFromLeft(width: Int, height: Int, isBlackPixel: (Int, Int) -> Boolean): Int {
        var columns = 0
        for (x in 0 until width) {
            if (!isBlackColumn(height, x, isBlackPixel)) break
            columns++
        }
        return columns
    }

    private fun blackColumnsFromRight(width: Int, height: Int, isBlackPixel: (Int, Int) -> Boolean): Int {
        var columns = 0
        for (x in width - 1 downTo 0) {
            if (!isBlackColumn(height, x, isBlackPixel)) break
            columns++
        }
        return columns
    }

    private fun isBlackRow(width: Int, y: Int, isBlackPixel: (Int, Int) -> Boolean): Boolean {
        var black = 0
        for (x in 0 until width) {
            if (isBlackPixel(x, y)) black++
        }
        return black.toFloat() / width >= BLACK_LINE_RATIO
    }

    private fun isBlackColumn(height: Int, x: Int, isBlackPixel: (Int, Int) -> Boolean): Boolean {
        var black = 0
        for (y in 0 until height) {
            if (isBlackPixel(x, y)) black++
        }
        return black.toFloat() / height >= BLACK_LINE_RATIO
    }

    private fun firstContentRow(width: Int, height: Int, isBlackPixel: (Int, Int) -> Boolean): Int? {
        for (y in 0 until height) {
            if (hasContentInRow(width, y, isBlackPixel)) return y
        }
        return null
    }

    private fun lastContentRow(width: Int, height: Int, isBlackPixel: (Int, Int) -> Boolean): Int? {
        for (y in height - 1 downTo 0) {
            if (hasContentInRow(width, y, isBlackPixel)) return y + 1
        }
        return null
    }

    private fun firstContentColumn(width: Int, height: Int, isBlackPixel: (Int, Int) -> Boolean): Int? {
        for (x in 0 until width) {
            if (hasContentInColumn(height, x, isBlackPixel)) return x
        }
        return null
    }

    private fun lastContentColumn(width: Int, height: Int, isBlackPixel: (Int, Int) -> Boolean): Int? {
        for (x in width - 1 downTo 0) {
            if (hasContentInColumn(height, x, isBlackPixel)) return x + 1
        }
        return null
    }

    private fun hasContentInRow(width: Int, y: Int, isBlackPixel: (Int, Int) -> Boolean): Boolean {
        var nonBlack = 0
        for (x in 0 until width) {
            if (!isBlackPixel(x, y)) nonBlack++
        }
        return nonBlack.toFloat() / width > CONTENT_LINE_RATIO
    }

    private fun hasContentInColumn(height: Int, x: Int, isBlackPixel: (Int, Int) -> Boolean): Boolean {
        var nonBlack = 0
        for (y in 0 until height) {
            if (!isBlackPixel(x, y)) nonBlack++
        }
        return nonBlack.toFloat() / height > CONTENT_LINE_RATIO
    }
}
