# ASS/SSA Support Matrix

**ID:** P4-ASS-001  
**Updated:** 2026-06-06  
**Scope:** Document the current `.ass / .ssa` parser, style metadata, rendering limits, and next validation work. This file tracks `P4-ASS-001` and the parser/model foundation of `P4-ASS-002`.

---

## Current Code Paths

| Entry point | Current behavior | Notes |
|-------------|------------------|-------|
| Local file subtitles | `SubtitleLoader.loadFromFile(...)` routes `.ass / .ssa` to `AssParser.parse(...)`. | Uses the current subtitle encoding preference before parsing. |
| Android file/content URI subtitles | `SubtitleLoader.loadFromUri(...)` routes `.ass / .ssa` to `AssParser.parse(...)` when the URI extension is available. | Falls back to UTF-8 or the configured encoding path. |
| Network subtitle URL | `SubtitleLoader.loadFromNetworkUrl(...)` routes `.ass / .ssa` URLs to `AssParser.parse(...)` after download and WebDAV subtitle cache lookup. | This is direct subtitle URL loading, not remote sidecar matching. |
| Player overlay | Parsed ASS lines are displayed through the same primary/secondary TextView subtitle path as SRT/VTT. | `SubtitleCueStyle` style metadata is carried on `SubtitleItem` and conservatively applied to TextView rendering. |

---

## Supported Today

| Area | Supported | Implementation detail |
|------|-----------|-----------------------|
| Section parsing | `[Events]` section scanning. | Parser enters events on `[Events]` and exits on the next bracketed section. |
| Cue parsing | `Dialogue:` rows. | Rows are split with comma `limit = 10`, matching the common ASS dialogue field shape. |
| Timing | `Start / End` time range parsing. | `H:MM:SS.CS` values are converted to milliseconds. |
| Text field | Dialogue text field extraction. | The parser uses field 10 after the limited split. |
| Line breaks | `\N / \n` are converted to newline characters. | Multi-line dialogue remains readable in the existing overlay. |
| Basic cleanup | ASS override tags are stripped. | Tags like `{\i1}` or `{\pos(...)}` are removed instead of rendered literally. |
| Style section parsing | `[V4+ Styles]` `Format:` and `Style:` rows are parsed for common fields. | Parsed fields are stored as `SubtitleCueStyle` style metadata on matching dialogue cues. |
| Dialogue format parsing | `[Events]` `Format:` can map Start, End, Style and Text positions. | The old default ASS dialogue format remains the fallback when no event format is present. |
| Style name matching | Dialogue `Style` references can attach a named style. | Matching is case-insensitive after trimming. |
| Common style metadata | `Fontname / Fontsize`, `PrimaryColour / SecondaryColour` family color inputs, `Outline / Shadow`, `Alignment / MarginL / MarginR / MarginV`. | Current model stores font name, font size, primary color, outline color, outline width, shadow depth, alignment and margins. |
| TextView style application | Font size, primary color, outline/shadow approximation and horizontal alignment. | `PlayerSubtitleCueStylePolicy` applies current cue style to primary and secondary subtitle TextViews, with global player subtitle settings as fallback. |
| Stability goal | Complex ASS text should degrade to readable plain text where possible. | Unsupported style data is ignored, not interpreted. |

---

## Ignored Or Unsupported Today

| ASS feature | Current status | Risk / reason |
|-------------|----------------|---------------|
| `[V4+ Styles]` style application | Partially rendered. | TextView rendering applies a conservative subset only. |
| `Style` field per dialogue row | Parsed when the dialogue references a known style. | Unknown styles fall back to plain text with `style = null`. |
| `Fontname / Fontsize` | Font size is rendered; font family is parsed but not rendered. | Typeface loading and font fallback are not implemented. |
| `PrimaryColour / SecondaryColour / OutlineColour / BackColour` | Primary and outline colors can be parsed; primary text color and outline/shadow color are rendered. | Secondary/back colors are not rendered. |
| `Outline / Shadow` | Rendered as TextView `setShadowLayer(...)` approximation. | This is not true ASS stroke drawing. |
| `Bold / Italic / Underline / StrikeOut` | Ignored. | Inline rich text spans are not preserved. |
| `Alignment / MarginL / MarginR / MarginV` | Horizontal alignment is rendered; margins are parsed but not rendered. | Existing overlay still applies one global subtitle position. |
| `\pos / \move` | Ignored and stripped as override tags. | Absolute positioning needs layout-aware rendering. |
| Karaoke timing tags | Ignored. | `Karaoke` effects need per-syllable timing and rendering state. |
| Drawing / vector clips | Ignored. | ASS drawing commands and vector clipping need a renderer beyond plain TextView. |
| Transform / fade / animation | Ignored. | `\t`, `\fad`, `\fade` and related animation tags are stripped with other override tags. |
| Layer / collision | Ignored. | Multiple cue layering and collision avoidance are not modeled. |
| Embedded fonts / attachments | Ignored. | Attachment sections are not parsed or loaded. |

---

## Remote Sidecar Boundary

WebDAV / NAS remote sidecar matching is deferred for ASS/SSA. Phase 3 documented remote same-directory matching for `.srt` / `.vtt` first, while ASS/SSA remains blocked on parser/rendering constraints and sample validation. Direct remote subtitle URLs may still be parsed if the user or caller provides a concrete `.ass / .ssa` subtitle URL to `SubtitleLoader`.

---

## Verification Samples

| Sample type | Expected result now | Needed before enhancement |
|-------------|---------------------|---------------------------|
| Simple ASS dialogue | Text appears with correct time range. | Keep as regression sample for parser stability. |
| ASS with font/color/outline style | Text appears with current cue font size/color and TextView shadow approximation. | Compare readability against global subtitle fallback. |
| ASS with `\pos`, `\move`, alignment and margins | Alignment and margins can be carried from style rows; inline position tags are still stripped. | Decide whether to support per-cue position or keep global-only. |
| ASS karaoke/effects/drawing | Plain text may appear; effects are ignored. | Confirm no crash and document readable fallback quality. |
| Remote WebDAV/NAS sidecar `.ass / .ssa` | Not auto-matched as a sidecar. | Revisit after local ASS style support is improved. |

---

## Follow-Up

- **P4-ASS-002 parser/model foundation:** Done. `SubtitleItem.style` carries optional `SubtitleCueStyle` metadata for common ASS style fields.
- **P4-ASS-002 rendering application MVP:** Done. Font size, primary color, outline/shadow approximation and horizontal alignment are conservatively applied to primary/secondary subtitle TextViews, with fallback to current global subtitle preferences.
- **Future ASS rendering:** true outline stroke, font family loading, per-cue margins/absolute positioning, transforms, karaoke and drawing remain unsupported.
- Keep sample validation separate from automatic remote ASS sidecar matching so network source behavior does not expand before the renderer can represent the format accurately.
