package com.openvideo.app.ui.player

object PlayerSessionQueueChromePolicy {
    fun shouldShowSessionListButton(queueSize: Int): Boolean = queueSize > 1

    fun canOpenSessionListPanel(queueSize: Int): Boolean = shouldShowSessionListButton(queueSize)
}
