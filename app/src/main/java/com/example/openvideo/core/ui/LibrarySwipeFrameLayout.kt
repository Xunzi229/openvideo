package com.example.openvideo.core.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

class LibrarySwipeFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onSwipeBack: (() -> Boolean)? = null

    private val edgePx = 24f * resources.displayMetrics.density
    private val minDragPx = 72f * resources.displayMetrics.density
    private var tracking = false
    private var intercepting = false
    private var startX = 0f
    private var startY = 0f

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tracking = event.x <= edgePx
                intercepting = false
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!tracking) return false
                val dx = event.x - startX
                val dy = abs(event.y - startY)
                if (dx > minDragPx / 2 && dx > dy) {
                    intercepting = true
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                tracking = false
                intercepting = false
            }
        }
        return intercepting
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - startX
                val dy = abs(event.y - startY)
                if (dx > minDragPx && dx > dy * 1.5f) {
                    val handled = onSwipeBack?.invoke() == true
                    tracking = false
                    intercepting = false
                    return handled
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val dx = event.x - startX
                val dy = abs(event.y - startY)
                tracking = false
                intercepting = false
                if (dx > minDragPx && dx > dy * 1.5f) {
                    return onSwipeBack?.invoke() == true
                }
            }
        }
        return tracking || intercepting
    }
}
