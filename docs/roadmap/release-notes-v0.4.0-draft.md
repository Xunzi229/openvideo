# OpenVideo v0.4.0 Release Notes Draft

Date: 2026-06-06

This records `P3-REL-001`: the Phase 3 release notes draft for the network sources version. It is a roadmap draft, not a tagged release. The current Gradle version remains controlled by `gradle.properties`.

## Headline

OpenVideo v0.4.0 focuses on network and multi-source playback while keeping the app offline-first and privacy-conscious. The release adds URL playback, protocol-specific network behavior, a unified Sources page, and a WebDAV MVP for browsing and playing remote files.

## Network and multi-source playback

- Added URL playback entry points from Home, Sources, share text, and browser/file-manager `VIEW` intents.
- URL validation now accepts `http`, `https`, and `rtsp`, normalizes scheme/host casing, and rejects unsupported or malformed input.
- Packaged Media3 support for HLS, DASH, and RTSP alongside HTTP/HTTPS progressive playback.
- Added protocol-aware buffering profiles for HTTP progressive, HLS/DASH adaptive streams, RTSP, and local files.
- Added network playback request header policy with OpenVideo User-Agent generation and sensitive header redaction.
- Added bounded retry for retryable network failures, with 1s / 2s / 4s backoff and no infinite retry loop.
- Added network status labels for buffering, reconnecting, and live / unknown-duration streams.
- Live or unknown-duration streams show `--:--` and avoid relative seek shortcuts when seeking is not safe.

## Sources

- Added a Sources page that groups local media, Open URL, WebDAV, saved sources, recent playback, and future media-server placeholders.
- Recent playback now merges local continue-watching entries with network URL entries without treating network URLs as missing local files.
- Saved URL sources show non-sensitive metadata, redacted display URLs, last-used time, validation, and a delete action.
- Delete and clear destructive actions use the shared confirmation sheet style documented in the design system.
- Source privacy copy explains where credentials are stored, what export excludes, and how diagnostics are redacted.

## WebDAV

- Added WebDAV source creation with base URL, username, password, and test-before-save behavior.
- WebDAV connection tests use `PROPFIND Depth: 0`; directory browsing uses `PROPFIND Depth: 1`.
- WebDAV credentials are stored with AndroidX Security `EncryptedSharedPreferences`; Room does not store passwords, tokens, cookies, or request headers.
- WebDAV browsing shows folders, playable video files, and regular files.
- WebDAV video playback injects a per-playback `Authorization` header instead of putting credentials in the URL.
- Same-directory `.srt` and `.vtt` subtitles are matched automatically for WebDAV videos.
- Short in-memory caching covers directory metadata and parsed remote subtitles through `WebDavMemoryCache`; video bytes are not cached.
- WebDAV errors now distinguish authentication failure, permission denied, not found, timeout, certificate failure, generic network failure, and generic bad status.

## Privacy

- Network source metadata stored in Room is non-sensitive.
- WebDAV passwords and future source credentials are expected to stay in encrypted storage.
- Settings export excludes sensitive markers such as password, token, cookie, authorization, and header.
- Display URLs and diagnostics must redact sensitive query values and credentials.
- Network subtitles and WebDAV playback reuse request headers without embedding credentials in media URLs.

## Not included

- No Jellyfin or Plex runtime integration.
- No SMB runtime dependency or SMB browsing in the production APK.
- No DLNA/UPnP runtime dependency, MediaServer browsing, or MediaRenderer casting.
- No WebDAV credential edit screen yet; users currently delete and recreate a source to change credentials.
- No persistent WebDAV offline cache, recursive WebDAV library scan, upload, rename, move, or delete.
- No generic cookie/header editor for arbitrary URL playback.
- No DRM license flow.

## Known issues

- DASH and RTSP need controlled end-to-end sample validation before claiming broad device/server compatibility.
- WebDAV 401/403/404/timeout/certificate paths are covered by policy/source tests, but device-level validation needs a controlled WebDAV sample account.
- Large WebDAV directories can produce large `PROPFIND Depth: 1` XML responses; pagination/lazy loading is not implemented.
- Android cleartext policy can block `http://` playback unless the environment explicitly allows it.
- Some live streams expose unknown duration or limited seek support; the UI intentionally disables unsafe relative seek behavior.
- Network servers that require cookies, DRM, custom auth, short-lived signed URLs, or provider-specific login flows may fail until source-specific integration exists.

## Rollback points

- If a protocol module causes a packaging or runtime regression, the Media3 optional module can be removed while preserving HTTP/HTTPS progressive playback.
- If automatic network retry causes regressions, disable or narrow `NetworkPlaybackRetryPolicy` and fall back to manual retry in the existing error HUD.
- If WebDAV browsing causes server compatibility problems, keep saved source metadata but hide or disable the browse entry until the parser/client path is patched.
- If credential handling regresses, disable WebDAV save/playback entry points before touching encrypted credential storage data.
- If Sources navigation regresses, keep Home Open URL as the minimal network playback entry point.

## Verification

Fresh verification for this draft should include:

- `gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`
- `git diff --check`
- adb install/start/pid/logcat smoke test
- Focused source tests for URL, network protocols, WebDAV connection/browse/subtitle/cache/error handling, and media-server decision docs.

Phase 3 sample validation still needs stable MP4/HLS/DASH/RTSP/WebDAV endpoints before the final public release notes should claim broad real-world compatibility.

## Supporting documents

- `docs/roadmap/network-source-privacy.md`
- `docs/roadmap/network-protocol-support-matrix.md`
- `docs/roadmap/webdav-usage-and-limits.md`
- `docs/roadmap/smb-nas-dlna-research.md`

