# Android TV Known Limits

Date: 2026-06-11

This records `P5-TV-LIMITS-001`: the Phase 5 Android TV known-limits note for release preparation. It summarizes release-facing limits from the current codebase and existing roadmap documents. It does not add runtime behavior.

## Summary

OpenVideo now has Android TV launch metadata, a TV home entry shell, basic D-pad focus order, remote playback keys, and a TV regression matrix. The app is not yet ready to claim full Android TV store readiness until the remaining TV-specific permission, visual asset, and device regression work is complete.

Related documents:

- [`tv-regression-matrix.md`](./tv-regression-matrix.md)
- [`network-protocol-support-matrix.md`](./network-protocol-support-matrix.md)
- [`webdav-usage-and-limits.md`](./webdav-usage-and-limits.md)
- [`smb-nas-dlna-research.md`](./smb-nas-dlna-research.md)

## Codec and format limits

- Playback uses the current Media3 pipeline and device decoders. Codec support still depends on the TV device, firmware, and installed decoder capabilities.
- Some audio formats, including DTS / DTS-HD / DTS-UHD, may need device decoder support or a future software fallback path. The current media info UI can surface codec diagnostics, but it does not guarantee playback for every audio codec.
- No DRM license flow is implemented. DRM-protected HLS/DASH streams remain out of scope for the current Android TV readiness baseline.
- Live or unknown-duration streams may not be seekable. The existing player policy intentionally renders unknown/live duration as `--:--` and avoids relative seek when duration is not available.

## Storage and permissions

- Local media access still depends on Android storage permission behavior. The Manifest declares `android.permission.READ_MEDIA_VIDEO` and `android.permission.READ_MEDIA_VISUAL_USER_SELECTED` for modern Android versions, plus legacy read access for older devices.
- TV home now has a basic first-run local-video permission explanation and OK-triggered system permission request. True remote-only completion of the system permission dialog still requires a TV device or TV emulator pass.
- If a user grants partial visual access on Android 14+, scan results may only include the selected videos. That is expected platform behavior, not a TV-specific media library guarantee.
- Permission loss follows the existing history policy: hidden fallback entries are not deleted solely because permission is missing; stale cleanup requires a successful scan.

## Network sources

- URL playback supports HTTP/HTTPS progressive, HLS, DASH, and RTSP as documented in `network-protocol-support-matrix.md`.
- Cleartext HTTP can still be blocked by Android network security policy. Prefer HTTPS samples for release validation unless testing the explicit cleartext error path.
- No generic cookie/header editor is exposed for arbitrary URL playback.
- WebDAV supports add/test, browse, playback, same-directory `.srt` / `.vtt` subtitles, and short in-memory metadata caching. WebDAV limitations from `webdav-usage-and-limits.md` still apply, including: No pagination or lazy loading for large directories, no edit-credentials screen, no recursive scan, no write operations, no offline video cache, and no real test account in the repo.
- SMB, Jellyfin, Plex, DLNA, and other media-server integrations are not shipped runtime sources in this phase. Per `smb-nas-dlna-research.md`: Do not add Jellyfin or Plex runtime dependencies to the APK in this slice.

## Remote and focus

- TV home focus order is explicitly defined for the current five entry cards, and the hidden permission-panel state has a Continue watching focus fallback. True Leanback launcher behavior and real D-pad traversal still require a TV device or TV emulator.
- Sources fixed rows, saved source rows, recent playback rows, WebDAV browser rows, the SourceDetail missing state, and the WebDAV browser empty state now have explicit `clickable` / `focusable` attributes and the shared `bg_focusable_card` focus ring. The reused Sources screen requests initial focus on the Local source row and refreshes Down navigation across saved/recent sections based on which lists are visible; saved source detail and WebDAV browser request initial focus on Back, saved source detail refreshes action focus order for URL vs WebDAV sources and falls back to a focusable missing-state message when the source is gone, and WebDAV browser Down from Back enters either the entry list or the empty state, but real D-pad traversal through these screens still requires a TV device or TV emulator pass.
- Current phone-class adb devices may only expose touchscreen features and may not support forcing television UI mode. Such devices are valid for install/start/logcat smoke only.
- Some reused phone/tablet screens are remote-operable through default focus work, and TV settings now gives retained rows an explicit default focus plus shortcuts into existing subtitle/audio/source screens. These screens are not yet redesigned as full 10-foot Leanback screens.

## TV home scope

- The TV home is an entry shell. Continue watching routes to the existing Home Recent category; Folders, Series, Sources, and Settings route to existing screens.
- The Continue watching card can show the first recent video thumbnail, title, and recent count, and the Series card can show the first available Phase 2 series poster. TV-specific Continue watching aggregation, video thumbnail carousel, automatic TV-specific thumbnail extraction, and detail hero artwork are not complete.
- TV mode now applies a basic `SettingsFragment` simplification for non-remote-first rows, gives retained rows a stable default focus, and exposes subtitle/audio/source shortcuts through existing screens. A dedicated 10-foot settings redesign remains incomplete.

## Release copy

Suggested wording for a pre-store internal build:

> Android TV support is in preview. Remote navigation, local/media-source playback, subtitles, audio selection, and settings reuse the current OpenVideo screens. Some TV-specific flows are still in progress, including first-run permission guidance, TV-optimized settings, store artwork, and full device regression on real Android TV hardware. Codec support depends on the device decoder, DRM streams are not supported, and network-source behavior follows the documented URL/WebDAV limits.

Do not use final store copy until:

- `tv-regression-matrix.md` has at least one real TV device or TV emulator pass recorded.
- First-run storage permission flow can be completed by remote.
- TV banner/icon/screenshot requirements are reviewed.
- Known codec, storage, and network limits are copied into the release notes.
