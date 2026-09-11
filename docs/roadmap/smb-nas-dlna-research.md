# SMB/NAS/DLNA Research

Date: 2026-06-06

## Scope

This records `P3-SMB-001`: SMB dependency research for a future NAS source. It does not implement SMB browsing or playback.

Do not add an SMB dependency to the APK in this slice. The WebDAV source already proves the source-management shape; SMB has larger protocol, licensing, random-access, and credential risks, so it should enter as an isolated prototype before becoming a shipped source.

## Recommendation

Recommended first prototype: SMBJ.

Use SMBJ only in an isolated experiment or branch first, with no dependency added to the production APK until these questions are answered:

- Can it connect, list directories, and read byte ranges from Windows Server, Samba, Synology/QNAP-style NAS, and guest-disabled shares?
- Can it provide a seekable data source for Media3 without buffering whole video files?
- Can same-directory subtitle lookup reuse the WebDAV sidecar matching rules without storing credentials in URLs or logs?
- Is the transitive dependency size acceptable after R8?
- Does Android TLS/crypto/provider behavior work across the project minSdk devices?

## Dependency Comparison

| Candidate | License | Android compatibility | Performance / streaming risk | Decision |
|---|---|---|---|---|
| SMBJ (`com.hierynomus:smbj`) | Apache-2.0 | Pure Java SMB2/SMB3 client, published on Maven Central. Android is not its primary advertised target, so it needs device validation. | JAR is about 608 KB for 0.14.0 before transitives. Need to verify random access, reconnect, and Media3 seek behavior. | Best first prototype because license is permissive and dependency is standard Maven. |
| jcifs-ng / Codelibs jcifs (`org.codelibs:jcifs`) | LGPL-2.1 family per package metadata | JVM library with current Maven artifacts, but Android behavior and modern SMB dialect coverage need verification. | Historical CIFS/jCIFS shape increases risk around SMB2/SMB3 performance, signing, and seek-heavy video reads. | Do not prototype first unless SMBJ fails; license and legacy API shape are less attractive for APK shipping. |
| smb-kotlin (`com.ctreesoft:smb-kotlin`) | Commercial license | Advertises JVM 17+ and Android API 26+, no JNI, SMB2/SMB3, coroutine API, streaming I/O. | Best Android-specific story, but paid/closed distribution affects open-source project fit and CI reproducibility. | Keep as fallback if paid dependency is acceptable later; do not add now. |
| libsmb2 | LGPL-2.1+ native library | SMB2/SMB3 userspace C library; Android would require JNI/NDK integration or a wrapper. | Strong async/zero-copy story, but native packaging, ABI splits, crash surface, and LGPL compliance add cost. | Defer unless Java/Kotlin options fail performance tests. |

## Prototype Shape

The prototype should live behind the existing source abstraction, not inside scanner or local media code:

- `core/network/smb/` for pure SMB policy and path parsing.
- A non-sensitive `media_sources` row with type `smb`; no username, password, domain, token, or headers in Room.
- Credentials in encrypted storage using the same pattern as WebDAV.
- Directory listing result mapped to the same UI model shape as WebDAV entries.
- Playback via a custom Media3 `DataSource` only if byte-range reads are proven; otherwise the prototype remains browse-only.

## Subtitle Feasibility

This records `P3-SMB-002`: NAS subtitle matching feasibility.

NAS subtitle matching should reuse the protocol-agnostic `RemoteSidecarSubtitleMatcher` rules where possible. WebDAV now delegates to that matcher, so SMB can later map an SMB directory entry into the same `Item` shape instead of duplicating sidecar rules.

- Match same directory only in MVP.
- Prefer exact basename (`movie.srt`) before language suffix (`movie.en.srt`).
- Start with `.srt` / `.vtt`; defer ASS/SSA until parser/rendering constraints are checked for remote sources.
- Do not store SMB credentials in subtitle URLs or diagnostics.

### Path encoding

SMB paths should be treated as structured server/share/path components, not concatenated display strings. The prototype should normalize path separators for matching, but preserve the SMB library's original path representation for reads. Matching should use decoded file names from the directory listing, while diagnostics should redact server, share, username, and domain when credentials or private hostnames may be present.

### Permission model

Permission model constraints:

- A subtitle candidate is valid only if it appears in the same directory listing as the video.
- Do not probe sibling folders for subtitles in the MVP; that would multiply permission failures and network round trips.
- If the video is readable but a subtitle read fails with access denied, playback should continue without the subtitle and show a non-fatal subtitle load failure.
- Cached subtitle metadata must be scoped by source and credential identity, following the WebDAV cache-key privacy pattern.

## Risks To Validate Before Shipping

- SMB random-access performance for large MKV/MP4 seeks.
- Reconnect behavior when Android dozes, Wi-Fi switches, or NAS sleeps.
- SMB signing/encryption compatibility across consumer NAS defaults.
- Domain/workgroup handling and guest access failures.
- APK size and method count after transitives.
- License obligations for LGPL candidates if distributed in Play Store builds.

## DLNA/UPnP Research

This records `P3-DLNA-001`: DLNA/UPnP research for the optional network-source track. It does not implement DLNA discovery, browsing, playback, or casting.

Do not add a DLNA or UPnP runtime dependency to the APK in this slice. DLNA has two separate product shapes that should not be mixed into the current WebDAV/NAS source work:

- Browse a DLNA MediaServer, read its `ContentDirectory`, and play the returned HTTP resource locally in OpenVideo.
- Control a remote `MediaRenderer` by discovering it and issuing AVTransport/RenderingControl actions, effectively adding a casting feature.

### Browsing vs casting boundary

Browsing should be treated as a future source adapter only if it can produce stable HTTP media URLs, titles, containers, and optional subtitle metadata that fit the existing source UI. The app remains the player, so playback can reuse the current Media3 URL path, network error HUD, retry policy, request headers, and history model where the server exposes direct HTTP resources.

Casting is a different feature family. A `MediaRenderer` flow would make OpenVideo a control point for another device, with remote playback state, volume, seek, renderer capability profiles, and failure modes that do not fit the current local-player source abstraction. It should move to a later TV/remote-control Phase if prioritized.

### Dependency comparison

| Candidate | License / maintenance | Android compatibility | Product fit | Decision |
|---|---|---|---|---|
| Cling (`org.fourthline.cling`) | LGPL/CDDL; project README marks Cling EOL and no longer actively maintained. | Advertises Java and Android support, but current artifacts are old and rely on custom Maven repository patterns. | Covers SSDP, UPnP control point, `ContentDirectory`, and renderer utilities. | Do not add to production now; acceptable only for an isolated spike if a maintained fork is selected. |
| CyberGarage UPnP (`org.cybergarage.upnp`) | Open source framework with older project style and personal Maven repository instructions. | Has Android sample history and notes Android multicast/emulator limits. | General UPnP stack, not a modern Android-first media-source adapter. | Lower priority than a maintained Cling fork; do not add now. |
| UPnPCast / maintained forks | MIT for the referenced Android casting library, but project scope is primarily casting replacement work. | Android-oriented, but dependency maturity and API stability need device validation. | Better fit for `MediaRenderer` casting than source browsing. | Keep for later casting Phase research, not Phase 3 source MVP. |
| Manual SSDP + SOAP prototype | Project-owned code, no new runtime dependency. | Smallest APK impact, but multicast, XML/SOAP, timeout, and device quirks must be implemented and tested. | Can limit scope to MediaServer browse-only and reuse existing HTTP playback. | Best future browse-only prototype if DLNA enters a later Phase. |

### Phase decision

Recommendation: defer DLNA to a later Phase.

Reasons:

- The current Phase 3 success criteria are already satisfied by URL, WebDAV, and source management; DLNA would add LAN multicast, SOAP, device profiles, and renderer control risk without improving the WebDAV MVP.
- `ContentDirectory` browsing is useful, but it overlaps with future Jellyfin/Plex native API work. Jellyfin already moved DLNA support to a first-party plugin rather than base install, so DLNA should not be the default Jellyfin/Plex integration path.
- `MediaRenderer` casting is a separate TV/remote workflow and should be designed with player state ownership, queue control, and renderer capability handling before implementation.

If reopened later, split the work into two explicit tracks:

1. `P*-DLNA-BROWSE`: browse-only MediaServer source using SSDP discovery and `ContentDirectory` Browse/Search, returning playable HTTP URLs to OpenVideo.
2. `P*-DLNA-CAST`: MediaRenderer control point using SSDP discovery plus AVTransport/RenderingControl, with remote playback UI and no assumption that local PlayerActivity owns playback.

### Risks to validate before shipping DLNA

- SSDP multicast reliability across Android Wi-Fi, VPN, power-saving modes, routers with IGMP snooping, and emulator limitations.
- Device description parsing and service version differences across UPnP Device Architecture 1.0/2.0 devices.
- `ContentDirectory` result paging, metadata quirks, protocolInfo mapping, transcoded vs direct-play URLs, and subtitle exposure.
- HTTP media URL lifetime, authentication, and whether URLs remain valid after the source list is refreshed.
- For casting, renderer seek/volume/state sync, renderer-specific codec profiles, and handling devices that expose partial AVTransport support.

## Media Server Follow-Up Decision

This records `P3-MEDIA-SERVER-001`: SMB/DLNA/Jellyfin/Plex follow-up decision record for the end of Phase 3. It does not implement media-server login, library sync, playback sessions, or SDK wiring.

Do not add Jellyfin or Plex runtime dependencies to the APK in this slice. The current Phase 3 source architecture should stop at URL and WebDAV runtime support plus research records for heavier LAN/media-server protocols.

### Recommendation

Recommendation: defer Jellyfin and Plex to a future Phase, and evaluate them as native API integrations rather than DLNA integrations.

Do not use DLNA as the default Jellyfin/Plex integration path. DLNA can expose a generic media-server browse surface, but it loses the product features that make Jellyfin and Plex useful:

- Authenticated users, access tokens, server discovery, remote access, and permissions.
- Rich library metadata such as posters, seasons, episodes, watched state, and resume points.
- Server-side transcoding decisions and direct play / direct stream selection.
- Stable item identifiers that can map to OpenVideo history and future cross-source progress.

Jellyfin native API should be the first open media-server prototype because the API and SDK ecosystem are open, self-hosted, and easier to test in CI or a local lab. Plex API should be researched after Jellyfin because account discovery, token handling, remote access, and product/API stability need a separate privacy and compatibility review.

### Future Phase split

Use separate tracks instead of one combined "media server" feature:

| Track | Goal | First slice | Out of scope for first slice |
|---|---|---|---|
| Jellyfin native API | Browse a Jellyfin library and open a direct-play URL in OpenVideo. | Login/token storage, server URL validation, library sections, item list, direct play URL handoff. | Full sync, transcoding controls, account cloud discovery, remote playback control. |
| Plex API | Decide whether Plex can be supported without brittle private assumptions. | Token privacy model, server discovery boundaries, library section read-only browse, direct play feasibility. | Plex account management, remote access setup, casting, watch-state sync. |
| DLNA browse-only | Generic LAN MediaServer source when native APIs are unavailable. | SSDP discovery plus `ContentDirectory` Browse returning HTTP URLs. | Jellyfin/Plex default path, casting, metadata-rich library sync. |
| DLNA casting | Remote `MediaRenderer` control point. | Dedicated TV/remote-control design. | Source browsing and local PlayerActivity ownership. |

### Security and privacy requirements

- Store media-server access tokens in encrypted storage only; do not store access tokens in Room, exported settings, logs, copied diagnostics, URLs, or crash reports.
- Keep server base URLs and library metadata separate from credentials.
- Redact tokens, session ids, user ids, and server addresses where diagnostics may leave the device.
- Treat transcoding URLs and direct play URLs as sensitive when they include query auth or short-lived tokens.
- If watch-state sync is added later, require an explicit user-facing toggle because it writes viewing activity back to a server.

### Playback decision rules

The future prototype should prefer direct play when the server exposes a stable playable URL compatible with the existing Media3 path. If the server requires transcoding, the first prototype may pass the server-provided transcoding URL to the existing URL playback path, but it should not add transcoding controls until server/session behavior is understood.

Jellyfin/Plex playback should reuse existing network playback policies where possible:

- `NetworkUrlPolicy` for final URL validation when applicable.
- `PlayerActivityIntents.networkPlayback(...)` for local playback handoff.
- Existing request-header privacy rules for per-playback headers.
- Existing network error HUD and bounded retry policy.
- Existing source privacy rules for credentials and diagnostics.

### Decision for Phase 3

Phase 3 is complete without Jellyfin/Plex runtime work. The follow-up decision is:

- Keep current source UI placeholders as future-facing copy only.
- Do not add native Jellyfin or Plex SDKs to the production APK yet.
- Do not route Jellyfin/Plex through DLNA by default.
- Start a future Phase with a Jellyfin native API read-only prototype before Plex.
- Revisit Plex only after token handling, server discovery, and direct play feasibility are documented.

## Sources

- SMBJ Maven Central 0.14.0 artifact: https://repo.maven.apache.org/maven2/com/hierynomus/smbj/0.14.0/
- SMBJ package metadata / Apache-2.0 summary: https://libraries.io/maven/com.hierynomus%3Asmbj
- Codelibs jcifs Maven Central versions: https://central.sonatype.com/artifact/org.codelibs/jcifs/2.1.40/versions
- jcifs license metadata: https://security.snyk.io/package/maven/org.codelibs%3Ajcifs
- smb-kotlin Android and pricing information: https://smbkotlin.com/
- libsmb2 project: https://github.com/sahlberg/libsmb2
- Cling project README: https://github.com/4thline/cling
- CyberGarage UPnP project: https://www.cybergarage.org/oss/cybergarage-upnp/
- UPnP Device Architecture discovery/SSDP spec: https://upnp.org/specs/arch/UPnPDA10_20000613.htm
- UPnP AV ContentDirectory service spec: https://www.upnp.org/specs/av/UPnP-av-ContentDirectory-v4-Service-20101231.pdf
- Jellyfin DLNA plugin documentation: https://jellyfin.org/docs/general/post-install/networking/dlna/
- UPnPCast Android library: https://github.com/yinnho/UPnPCast
- Jellyfin API documentation: https://api.jellyfin.org/
- Jellyfin Kotlin SDK: https://github.com/jellyfin/jellyfin-sdk-kotlin
- Plex developer portal: https://developer.plex.tv/
