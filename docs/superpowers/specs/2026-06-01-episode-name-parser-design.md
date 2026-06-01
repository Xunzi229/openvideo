# Episode Name Parser Design

## Context

Phase 2 moves OpenVideo from a file list toward a local media library. The first low-risk slice is a pure episode-name parser that can later feed series grouping, subtitle matching 2.0, smart lists, and playlist ordering without changing Room, UI, or playback behavior in this increment.

The current code already has `PlayerEpisodeOrderingPolicy` for queue order, but that policy is playback-specific and only answers whether a queue can be ordered. The new parser should be a reusable metadata component that extracts a normalized show title, season, episode range, confidence, and rule reason from a filename.

## Scope

This slice adds a pure Kotlin parser under `core/metadata` and focused unit tests.

In scope:

- Parse common episode patterns: `S01E02`, `1x02`, `EP02`, `E02`, Chinese `di 02 ji` names, and `S01E01-E02`.
- Clean noisy tokens from inferred titles, including file extensions, resolution, source, codec, release group brackets, and common audio/subtitle markers.
- Return conservative confidence values so future grouping can auto-use high confidence, inspect medium confidence, and ignore low confidence.
- Keep the parser Android-independent and deterministic.

Out of scope:

- Room schema changes, identity migration, series tables, and episode detail UI.
- Changing folder queue ordering or playback behavior.
- Online metadata lookup, TMDb, poster lookup, or user-editable series metadata.

## API

Add:

```kotlin
data class EpisodeMatch(
    val title: String,
    val season: Int?,
    val episodeStart: Int,
    val episodeEnd: Int?,
    val confidence: EpisodeMatchConfidence,
    val rule: String
)

enum class EpisodeMatchConfidence {
    HIGH,
    MEDIUM,
    LOW
}

object EpisodeNameParser {
    fun parse(fileName: String, parentFolderName: String? = null): EpisodeMatch?
}
```

`title` is the cleaned display candidate. `season` is nullable because `EP02` and Chinese episode markers often omit a season. `episodeEnd` is only present for multi-episode files. A `null` result means the parser found no useful episode signal.

## Matching Rules

Rules run from most specific to least specific:

1. `SxxEyy` and `SxxEyy-Ezz` produce high-confidence matches.
2. `1x02` produces high confidence.
3. `EP02`, `E02`, and Chinese episode markers such as `\u7b2c02\u96c6` produce medium confidence when a usable title can be inferred.
4. If the filename title is empty after cleanup, fall back to the parent folder name and lower the confidence by one level.
5. Pure numeric filenames and movie-style names such as `Movie.Name.2024.1080p.mkv` do not return a match unless an explicit episode token exists.

Noise cleanup is intentionally conservative. It removes common media tokens but does not try to solve every release naming style. When cleanup would leave an empty or one-character title without a parent folder fallback, the result is rejected.

## Error Handling

The parser never throws for malformed names. Blank filenames, invalid episode numbers, reversed ranges, and overflow-like numbers return `null`. Regex matches should be bounded to normal filename-length strings and must not depend on locale-specific global state.

## Tests

Add `EpisodeNameParserTest` with cases for:

- `Show.Name.S01E02.1080p.WEB-DL.x264.mkv`.
- `Show.Name.1x02.HDTV.mkv`.
- `Show.Name.S01E01-E02.mkv`.
- A Chinese drama filename containing `\u7b2c02\u96c6` and `1080P`.
- A bracketed anime filename with release-group, title, episode `03`, resolution, and bilingual subtitle markers.
- `EP12.mkv` with parent folder fallback.
- Movie names with years and no episode token returning `null`.
- Pure numeric filenames returning `null`.
- Invalid or reversed episode ranges returning `null`.

## Verification

Run the focused unit test first, then the app unit-test baseline if time allows:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.openvideo.core.metadata.EpisodeNameParserTest"
.\gradlew.bat :app:testDebugUnitTest
git diff --check
```

## Roadmap Update

After implementation, update Phase 2 to mark `P2-EP-001 EpisodeNameParser` as started or complete for the pure parser slice. Do not mark series grouping or media identity as complete.
