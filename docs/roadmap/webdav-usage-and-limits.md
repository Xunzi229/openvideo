# WebDAV Usage And Known Limits

Date: 2026-06-06

This records `P3-WD-DOC-001`: the Phase 3 WebDAV MVP usage guide and known limitations. It documents the current implementation; it does not add new WebDAV behavior.

## Current Scope

WebDAV MVP supports:

- Add a WebDAV source with base URL, username, and password.
- Test the source before saving with `PROPFIND Depth: 0`.
- Browse directories with `PROPFIND Depth: 1`.
- Play video files returned by the WebDAV directory listing.
- Inject per-playback `Authorization` headers for protected video and subtitle URLs.
- Match same-directory subtitles with `.srt` and `.vtt` extensions.
- Keep short in-memory metadata caches through `WebDavMemoryCache`.

WebDAV is exposed as a network source. It is not a general cloud sync client, downloader, offline cache, or file manager.

## Add a WebDAV source

1. Open the Sources page.
2. Choose WebDAV.
3. Enter the WebDAV folder base URL, username, and password.
4. Submit the form. OpenVideo normalizes the base URL and rejects unsupported schemes, missing hosts, embedded username/password, query strings, and fragments.
5. OpenVideo sends a `PROPFIND Depth: 0` request through `WebDavConnectionClient`.
6. If the server returns `200` or `207`, the source metadata is saved and the credentials are stored separately.

Credential behavior:

- `WebDavCredentialStore` stores username and password in AndroidX Security `EncryptedSharedPreferences`.
- Room does not store passwords, tokens, cookies, or request headers.
- The Room `media_sources` row stores only non-sensitive metadata such as type, display name, base URL, and timestamps.
- Playback URLs must not contain embedded username/password.

## Browse and play

Opening a saved WebDAV source enters `WebDavBrowserFragment`:

- The fragment reads credentials through the repository and `WebDavCredentialStore`.
- `WebDavConnectionClient.listDirectory(...)` sends `PROPFIND Depth: 1`.
- `WebDavDirectoryParser` parses `207 Multi-Status` responses into folders, playable video entries, and regular files.
- Tapping a folder opens that folder URL in another browser fragment instance.
- Tapping a playable video starts `PlayerActivity` through `PlayerActivityIntents.networkPlayback(...)`.
- `WebDavBrowserFragment` passes an `Authorization: Basic ...` request header for the playback session instead of placing credentials in the URL.

Non-playable files remain visible as files, but tapping them shows a non-playable message rather than attempting playback.

## same-directory subtitles

WebDAV subtitle discovery is intentionally narrow:

- Only same-directory subtitles are considered.
- `.srt` and `.vtt` are included in the MVP.
- Exact basename matches such as `movie.srt` are preferred.
- Language suffix matches such as `movie.en.srt` are accepted after exact matches.
- Directories, unrelated files, and ASS/SSA files are ignored in this phase.

The matching logic is implemented through `WebDavSubtitleMatcher`, which delegates to the protocol-agnostic `RemoteSidecarSubtitleMatcher`. Network subtitle loading reuses the current playback request headers, so protected WebDAV subtitles do not need credentials in their URL.

## Cache and clearing behavior

`WebDavMemoryCache` is a process-local metadata cache:

- Directory listing metadata is cached for 60 seconds.
- Parsed remote subtitle entries are cached for 5 minutes.
- Video bytes are not cached.
- Cache keys hash URL and request-header identity; they do not store plaintext `Authorization`, password, or header values.
- The Settings clear-cache action also clears the WebDAV in-memory cache.

This cache improves repeated browsing and subtitle loading inside the running app process. It is not persistent and should not be treated as offline availability.

## Error handling

WebDAV connection and browse failures are classified by `WebDavConnectionPolicy`:

| Case | Current user-facing direction |
|---|---|
| `401` | Username or password is incorrect. |
| `403` | Account cannot access the folder. |
| `404` | Folder or file was not found. |
| Timeout | Server took too long to respond. |
| TLS / certificate error | Server certificate could not be verified. |
| Other network failure | Server cannot be reached. |
| Other HTTP status | Generic WebDAV test failure. |

Playback errors after a video is opened continue through the normal player network error HUD and retry behavior.

## Known limits

- No edit credentials screen exists yet. Users currently need to delete and recreate the source to change credentials; the next source-editing slice should add edit credentials and retry.
- No pagination or lazy loading exists for a large directory. `PROPFIND Depth: 1` can produce large XML responses on folders with many files.
- No background download, offline video cache, or persistent directory cache is implemented.
- No write operations are supported. OpenVideo does not upload, rename, move, or delete remote WebDAV files.
- No recursive library scanning is implemented for WebDAV sources.
- No manual remote subtitle picker is implemented for sibling folders or differently named subtitle files.
- ASS/SSA remote subtitle matching is deferred.
- Server compatibility varies. Redirects, non-standard WebDAV XML, path encoding differences, self-signed certificates, and provider-specific auth flows may need targeted follow-up.
- HTTP cleartext behavior still follows Android network security policy.
- There is no real test account in the repo, so 401/403/404/timeout/certificate end-to-end device validation needs a controlled WebDAV sample server.

## Implementation anchors

- `WebDavConnectionClient`: test connection and directory listing.
- `WebDavConnectionPolicy`: URL validation, credential validation, `PROPFIND` request construction, and error classification.
- `WebDavDirectoryParser`: `207 Multi-Status` parsing.
- `WebDavCredentialStore`: encrypted credential storage.
- `WebDavBrowserFragment`: browse UI, folder navigation, playback launch, and per-playback `Authorization` header injection.
- `WebDavSubtitleMatcher`: same-directory subtitle matching.
- `WebDavMemoryCache`: short in-memory directory and subtitle metadata cache.

