package com.example.openvideo.core.ui

/**
 * Union of system bars and display cutout so notches, punch-holes,
 * waterfalls, and OEM gesture bars do not cover content.
 */
object SystemBarInsetsPolicy {

    data class Edges(val left: Int, val top: Int, val right: Int, val bottom: Int)

    fun union(systemBars: Edges, cutout: Edges): Edges = Edges(
        left = maxOf(systemBars.left, cutout.left).coerceAtLeast(0),
        top = maxOf(systemBars.top, cutout.top).coerceAtLeast(0),
        right = maxOf(systemBars.right, cutout.right).coerceAtLeast(0),
        bottom = maxOf(systemBars.bottom, cutout.bottom).coerceAtLeast(0)
    )
}
