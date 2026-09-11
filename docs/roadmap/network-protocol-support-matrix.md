# Network Protocol Support Matrix

Date: 2026-06-06

This records `P3-MATRIX-001`: the Phase 3 URL/HLS/DASH/RTSP support matrix. It documents what the current code path supports; it does not add new playback behavior.

## Summary

OpenVideo accepts `http`, `https`, and `rtsp` playback URLs through `NetworkUrlPolicy`. Playback goes through `PlayerActivityIntents.networkPlayback(...)`, `PlayerViewModel`, and the Media3 player pipeline. HLS, DASH, and RTSP support is packaged through explicit Media3 modules; HTTP/HTTPS progressive playback uses the base ExoPlayer HTTP data source.

| Protocol / format | URL shape | Media3 module | Buffering profile | Current status | Known limits |
|---|---|---|---|---|---|
| HTTP/HTTPS progressive | `http://...` / `https://...` direct media files such as MP4/MKV/WebM | Base `media3-exoplayer` with OkHttp/HTTP data source path | `NETWORK_PROGRESSIVE` | Supported for direct playable resources. Network errors use the Phase 3 network HUD and retry policy. | Cleartext HTTP can be blocked by Android network security policy. Servers that require cookies, DRM, custom auth, or expiring signed URLs need source-specific handling. |
| HLS | `.m3u8` playlist URLs, with query/fragment ignored for profile detection | `media3-exoplayer-hls` | `ADAPTIVE_STREAM` | Supported as URL playback. Live or unknown duration streams render duration as `--:--` and disable relative seek shortcuts when duration is not seekable. | Broken playlists, unsupported segments/codecs, geo restrictions, DRM, and expiring keys remain source failures surfaced through Media3/network errors. |
| DASH | `.mpd` manifest URLs, with query/fragment ignored for profile detection | `media3-exoplayer-dash` | `ADAPTIVE_STREAM` | Supported as URL playback through the packaged DASH module. | DRM, uncommon manifests, codec gaps, and server auth are not solved by Phase 3. |
| RTSP | `rtsp://...` camera or stream URLs | `media3-exoplayer-rtsp` | `RTSP_STREAM` | Supported as URL playback and not treated as a local file path. Uses a lower startup buffer profile than HTTP/HLS/DASH. | Device/server compatibility varies. Authentication, NAT/firewall, transport mode differences, and camera-specific quirks need sample validation per device. |

## Implemented Code Paths

- URL validation: `core/network/NetworkUrlPolicy` allows only `http`, `https`, and `rtsp`, rejects missing scheme/host, whitespace/control characters, and unsupported schemes.
- Playback intent: `ui/player/PlayerActivityIntents.networkPlayback(...)` passes normalized URLs into `PlayerActivity` with stable URL-derived IDs and optional request headers/subtitles.
- Packaged protocol modules: `app/build.gradle.kts` includes `media3-exoplayer-hls`, `media3-exoplayer-dash`, and `media3-exoplayer-rtsp`.
- Buffering profile: `core/player/PlayerBufferingPolicy` maps HTTP/HTTPS direct resources to `NETWORK_PROGRESSIVE`, `.m3u8` and `.mpd` to `ADAPTIVE_STREAM`, and `rtsp://` to `RTSP_STREAM`.
- Error behavior: `NetworkErrorClassifier`, `NetworkPlaybackRetryPolicy`, and player HUD integration classify DNS/connection/timeout/HTTP/cleartext failures and apply bounded automatic retry only where appropriate.
- Live / unknown duration: `PlaybackTimelinePolicy` prevents relative seek when duration is unknown or live-like, and `PlayerTimeline` renders unknown/live duration as `--:--`.

## Cleartext HTTP

Cleartext HTTP is not globally enabled by this matrix. The Android network security policy can block `http://` playback on modern devices. When blocked, the player should surface the existing cleartext network error instead of treating the stream as a generic decode or local-file failure. Prefer `https://` samples for release validation unless a local test server intentionally validates the cleartext-blocked path.

## Verification samples

Keep sample validation lightweight and reproducible. Do not commit private, signed, tokenized, account-bound, or unstable media URLs.

| Kind | Sample policy | Expected result to record |
|---|---|---|
| HTTP/HTTPS progressive | Public direct MP4 or local LAN test server. Prefer HTTPS for success; use local HTTP only to validate cleartext behavior. | Opens `PlayerActivity`; either plays or reports a specific network/HTTP/cleartext error. |
| HLS | Public HLS test playlist without DRM. | Opens `PlayerActivity`; VOD has normal seek, live/unknown duration shows `--:--`. |
| DASH | Public DASH test manifest without DRM. | Opens `PlayerActivity`; unsupported codec/DRM failures stay classified as playback/network failures. |
| RTSP | Local camera/test RTSP endpoint when available. | Opens `PlayerActivity`; failures should not be misclassified as a local missing file. |

## Known limits

- No DRM license flow is implemented in Phase 3.
- No generic cookie/header editor is exposed for arbitrary URL playback. WebDAV injects per-playback `Authorization` headers through its own credential path.
- No promise is made that every live stream is seekable; unknown duration and live-like streams intentionally disable relative seek shortcuts.
- No background download/offline caching is added for network video.
- RTSP compatibility must be validated against real cameras/servers because server behavior varies more than HTTP playback.
- URLs shown in UI and diagnostics must remain redacted according to `NetworkRecentUrlPolicy` and the network-source privacy rules.

