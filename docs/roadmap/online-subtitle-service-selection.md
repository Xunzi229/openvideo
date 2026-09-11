# P4-ONLINE-001 Online Subtitle Service Selection

**Date:** 2026-06-06

This record completes Phase 4 `P4-ONLINE-001`. It chooses a service direction for online subtitle search without adding production runtime code, SDK dependencies, account storage, or automatic network lookup.

## Selected direction

Use **OpenSubtitles.com** as the first integration target for `P4-ONLINE-002`, behind a manual search flow only.

Reasons:

- It is the most directly aligned subtitle catalog for a video player MVP.
- The API surface covers subtitle search and download flows needed by a manual title / season / episode / language search.
- It has explicit authentication, quota, and client identification requirements, which are easier to document and gate behind user action than unofficial scraping.

Do not integrate a fallback scraper or unofficial service in this phase. A compatible service can be evaluated later only if it has clear terms, stable API behavior, and a privacy model that can be explained in the UI.

## OpenSubtitles.com constraints

Current selection assumptions from the official OpenSubtitles API/help pages:

- API access requires an **API key**.
- Requests must identify the client with a **User-Agent**.
- Some flows require user account login or token-based access.
- Downloads are subject to account / plan limits and service-side rate limits.
- The integration must expect HTTP errors for unauthorized requests, quota exhaustion, missing results, and network failures.

References:

- https://ai.opensubtitles.com/docs
- https://opensubtitles.tawk.help/article/getting-started
- https://www.opensubtitles.com/en/support_us

## Privacy boundary

OpenVideo remains offline-first. Online subtitle search must be user initiated and transparent.

Requirements for `P4-ONLINE-002` and later:

- **manual search only** from an explicit player subtitle action.
- **No automatic lookup when opening a video**.
- **Do not upload file hash by default**.
- The first-use prompt must explain what can be sent: query title, optional season/episode, language, and selected service.
- File path, folder path, playback history, WebDAV credentials, request headers, cookies, tokens, and local scan data must not be sent.
- API key and user token storage must be designed separately before real account login ships.
- Diagnostics must redact query text if it contains a URL, path, token, cookie, or authorization header.

## P4-ONLINE-002 scope

The next slice should implement only a manual-search foundation:

- A pure request model for title, season, episode, and language.
- A mockable client boundary for service requests.
- A result model with language, release/file name, source, and download metadata.
- UI copy that makes network use explicit before the first request.
- Tests proving no search starts from video open or automatic subtitle load.

Out of scope for `P4-ONLINE-002`:

- Real API key bundled in the APK.
- Background subtitle search.
- Automatic file hash upload.
- Account login persistence.
- Download cache cleanup UI.
- Secondary subtitle auto-selection.

## Rejected options for this slice

| Option | Decision | Reason |
|--------|----------|--------|
| OpenSubtitles.com immediate runtime integration | Rejected for this slice | Needs user-facing privacy prompt, API key handling, request model, and mock tests first. |
| Scraping subtitle websites | Rejected | Fragile, unclear terms, high maintenance, and difficult to explain privacy behavior. |
| Bundling multiple providers now | Rejected | Adds product and QA scope before the manual-search privacy boundary is proven. |
| Local hash-first matching | Rejected by default | Roadmap explicitly requires no default hash upload; hash can only be an explicit later option. |

## Acceptance notes

`P4-ONLINE-001` is complete when this document exists, Phase 4 roadmap marks the selection as done, and tests prove no OpenSubtitles runtime dependency is added during the selection slice.
