package com.example.openvideo.core.ui

import android.app.Activity
import androidx.window.layout.WindowMetricsCalculator

object WindowSizeHelper {

    fun computeBreakpoint(activity: Activity): ScreenBreakpoint {
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(activity)
        val widthDp = metrics.bounds.width() / activity.resources.displayMetrics.density
        return when {
            widthDp >= 840f -> ScreenBreakpoint.EXPANDED
            widthDp >= 600f -> ScreenBreakpoint.MEDIUM
            else -> ScreenBreakpoint.COMPACT
        }
    }
}
