package com.openvideo.app.ui.player

object CompatibilityRequestPolicy {
    /** LibVLC's media API exposes these options, but not arbitrary HTTP headers. */
    fun supports(headers: Map<String, String>): Boolean = headers.all { (name, value) ->
        value.isBlank() || name.equals("User-Agent", true) || name.equals("Referer", true)
    }
}
