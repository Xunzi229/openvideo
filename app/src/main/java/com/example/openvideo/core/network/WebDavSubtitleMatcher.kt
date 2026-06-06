package com.example.openvideo.core.network

object WebDavSubtitleMatcher {
    fun matchForVideo(
        video: WebDavDirectoryParser.Entry,
        entries: List<WebDavDirectoryParser.Entry>
    ): WebDavDirectoryParser.Entry? {
        val match = RemoteSidecarSubtitleMatcher.matchForVideo(
            video = video.toRemoteSidecarItem(),
            candidates = entries.map { entry -> entry.toRemoteSidecarItem() }
        ) ?: return null
        return entries.firstOrNull { entry -> entry.url == match.url }
    }

    private fun WebDavDirectoryParser.Entry.toRemoteSidecarItem(): RemoteSidecarSubtitleMatcher.Item =
        RemoteSidecarSubtitleMatcher.Item(
            name = name,
            url = url,
            isDirectory = isDirectory,
            isPlayableVideo = isPlayableVideo
        )
}
