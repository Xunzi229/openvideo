# Media Identity Foundation Design

## Context

Phase 2 needs stable media identity before Room schema changes, history migration, series grouping, and playlist identity wiring. The next low-risk slice is a pure foundation layer: normalize media paths and build deterministic fingerprints from already available local video metadata.

This slice does not create tables or change repository behavior. It creates reusable policy code that later database and repository work can depend on.

## Scope

In scope:

- Add `core/mediaid/MediaPathNormalizer`.
- Add `core/mediaid/MediaFingerprintPolicy`.
- Normalize local file paths, Windows-like separators, trailing separators, repeated slashes, and content URI strings.
- Keep case handling conservative: preserve display case but expose a comparison key where file/content URI paths are lowercased for matching.
- Build a deterministic `MediaFingerprint` from title, path, size, duration, width, height, and modified/date-added style timestamp.
- Return stable matching signals for later identity matching without deciding merge policy in this slice.

Out of scope:

- Room `media_identity` tables and migrations.
- Updating `HistoryEntity`, favorites, playlists, or scanner persistence.
- UI for possible file moves or merge conflicts.
- Hashing file bytes or reading storage.

## API

Add:

```kotlin
data class NormalizedMediaPath(
    val original: String,
    val displayPath: String,
    val comparisonKey: String,
    val fileName: String,
    val parentKey: String
)

object MediaPathNormalizer {
    fun normalize(pathOrUri: String): NormalizedMediaPath?
}

data class MediaFingerprint(
    val normalizedPathKey: String,
    val normalizedTitleKey: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val timestamp: Long
)

object MediaFingerprintPolicy {
    fun fromFields(
        title: String,
        pathOrUri: String,
        sizeBytes: Long,
        durationMs: Long,
        width: Int,
        height: Int,
        timestamp: Long
    ): MediaFingerprint?
    fun strongMatch(left: MediaFingerprint, right: MediaFingerprint): Boolean
    fun likelyRename(left: MediaFingerprint, right: MediaFingerprint): Boolean
}
```

The implementation can choose concrete parameter names, but it should stay Android-independent and avoid depending on `VideoItem` directly so JVM tests remain simple.

## Behavior

Path normalization:

- Blank input returns `null`.
- Backslashes become `/`.
- Repeated separators collapse.
- Trailing separators are removed except for URI roots.
- File names and parent keys are derived from the normalized display path.
- `comparisonKey` lowercases using `Locale.ROOT` for matching.

Fingerprint policy:

- Invalid fingerprints return `null` when size or duration is non-positive.
- `strongMatch` requires same normalized path key plus same size and duration.
- `likelyRename` requires different path keys but same size, duration, and dimensions; timestamp may differ because file moves can rewrite MediaStore dates.
- The policy does not merge anything. It only answers match questions for future repository code.

## Tests

Add focused tests for:

- Android file path normalization.
- Backslash and repeated separator cleanup.
- Trailing separator cleanup.
- Content URI comparison keys.
- Blank path rejection.
- Fingerprint creation rejecting invalid size/duration.
- Strong match for same path, size, and duration.
- Rename-like match for different paths with same size/duration/dimensions.
- Non-match when size or duration differs.

## Roadmap Updates

After each implementation step:

- Update `docs/roadmap/phases/phase-2-media-library/README.md`.
- Update `docs/roadmap/ROADMAP.md` when a coherent sub-slice is complete.
- Do not mark Room identity, history migration, playlist identity, series grouping, artwork, or smart lists complete.
