# Home Experience Handoff

Updated: 2026-05-24.

This note records Home page behavior that should be treated as product rules instead of incidental
implementation details. The goal is to keep later Home/Recent/Favorites work from accidentally
collapsing category-specific behavior back into one shared global state.

## Current Home Baseline

Home currently has three top-level categories inside the shared Home surface:

- `ALL`
- `RECENT`
- `FAVORITES`

These categories share one page shell, but they do **not** share all interaction state.
Their top chips show the current filtered list count for each category, so the user can see at a
glance whether Recent/Favorites have matching items before switching.

## Locked Product Rules

### 1. View mode memory is per category, not global

This is now a deliberate feature, not an implementation accident.

- `ALL` remembers its own layout mode.
- `RECENT` remembers its own layout mode.
- `FAVORITES` remembers its own layout mode.
- Switching one category between list/grid must not change the other two categories.
- Returning to a category must restore that category's last-used layout mode immediately.

Current persistence keys:

- `AppPrefs.homeAllViewMode`
- `AppPrefs.homeRecentViewMode`
- `AppPrefs.homeFavoriteViewMode`

Current wiring:

- [`AppPrefs.kt`](E:/github/openvideo/app/src/main/java/com/example/openvideo/core/prefs/AppPrefs.kt)
- [`HomeViewModel.kt`](E:/github/openvideo/app/src/main/java/com/example/openvideo/ui/home/HomeViewModel.kt)
- [`HomeFragment.kt`](E:/github/openvideo/app/src/main/java/com/example/openvideo/ui/home/HomeFragment.kt)

Guardrail:

- Do not route Home layout state back through a single global `appPrefs.viewMode` write path.
- If a future refactor introduces shared view-state helpers, category-specific Home layout memory
  must remain preserved.

### 2. RECENT order is fixed by latest playback, not user sort

`RECENT` is continue-watching driven and should stay stable:

- latest playback appears at the top
- it does not participate in the generic sort-field/sort-order pipeline
- it still keeps the right-side layout toggle visible
- only the sort controls are hidden for `RECENT`

Guardrail:

- Do not feed `RECENT` back into the same sort controls used by `ALL`/`FAVORITES` unless product
  requirements explicitly change.

### 3. RECENT and FAVORITES are distinct categories with distinct UI memory

Even though they share the same fragment shell, they should be treated as separate surfaces for
user-facing memory:

- category switch restores the target category's layout mode
- category-specific badges or metadata can be added without forcing the same presentation on other
  categories
- future per-category UX memory should prefer category-keyed persistence over one shared Home key

## Existing Test Guardrails

These tests now intentionally lock the behavior above:

- [`HomeRecentSourceTest.kt`](E:/github/openvideo/app/src/test/java/com/example/openvideo/ui/home/HomeRecentSourceTest.kt)
- [`HomeRecentSortUiSourceTest.kt`](E:/github/openvideo/app/src/test/java/com/example/openvideo/ui/home/HomeRecentSortUiSourceTest.kt)
- [`HomeCategoryViewModeSourceTest.kt`](E:/github/openvideo/app/src/test/java/com/example/openvideo/ui/home/HomeCategoryViewModeSourceTest.kt)
- [`HomeCategoryViewModeUiSourceTest.kt`](E:/github/openvideo/app/src/test/java/com/example/openvideo/ui/home/HomeCategoryViewModeUiSourceTest.kt)
- [`HistoryContinueWatchingLabelsSourceTest.kt`](E:/github/openvideo/app/src/test/java/com/example/openvideo/ui/history/HistoryContinueWatchingLabelsSourceTest.kt)
- [`HistoryCleanupPolicyTest.kt`](E:/github/openvideo/app/src/test/java/com/example/openvideo/ui/history/HistoryCleanupPolicyTest.kt)
- [`HomeHistoryCleanupSourceTest.kt`](E:/github/openvideo/app/src/test/java/com/example/openvideo/ui/home/HomeHistoryCleanupSourceTest.kt)

If Home category behavior is touched later, update these tests intentionally instead of bypassing
them.

## Continue Watching Labels

Updated: 2026-05-16.

- Continue-watching progress and relative-time labels now come from string resources through
  `HistoryContinueWatchingLabels`, shared by Home `RECENT`, History, and
  `HistoryContinueWatchingPolicy`.
- English defaults live in `values/strings.xml`; Simplified Chinese in `values-zh-rCN/strings.xml`.
- Unit tests use `HistoryContinueWatchingLabels.englishDefaults()`; UI layers load localized
  labels from `Context`.

## History Cleanup After Scan

Updated: 2026-05-16.

- After a published MediaStore scan, `VideoRepository.pruneStaleHistory()` removes history rows only
  when the file is missing locally and the video no longer appears in the latest scan by id or
  normalized path.
- Rows that are temporarily offline but still on disk, or still indexed by the latest scan, are kept
  so RECENT can continue to show recoverable missing-file states until the entry is truly stale.

## Folder, Search, And Scan Polish

Updated: 2026-05-16.

- Folder filter chips: `VideoFolderFilterPolicy` hides zero-count folders, pins long-pressed
  folders to the front, sorts each pinned/unpinned group by video count descending before
  name/key tie-breaks, and prunes stale pinned keys after every published scan. Pinned state is
  persisted in `AppPrefs.pinnedFolderKeys` and shared by Home chips + Local folder rows.
- Search: `MediaLibrarySearchPolicy` matches title, full path, file name, and parent folder name
  for the active query.
- Advanced filters: `MediaLibraryAdvancedFilters` adds optional duration (short/medium/long),
  format (mp4/mkv/avi/webm), and date-added (7d / 30d / older) buckets behind one filter button on
  the Home sort row. The button tints accent when any filter is active. Filters apply to all
  category lists, folder chips, and empty state pipeline at the same time.
- Scan progress: first-run full scan shows an indeterminate progress bar + live scanned-count label
  through `MediaLibraryScanLoadingUi`, driven by `VideoScanOutcome.Progress`. Incremental refreshes
  do not show progress to avoid noise.
- Scanner robustness: `VideoRepository` shares a single `scanVideos()` flow via `shareIn`, so Home
  + Local Fragments share one observer + one scan; deletes invalidate the scanner cache; large diff
  fetches are batched in groups of 200 IDs; queries use the active `videoCollectionUri()` instead of
  `EXTERNAL_CONTENT_URI` on Android 10+.

## Next Safe Extension Points

If Home work continues, these are safe follow-ups:

- add more category-specific display memory if product asks for it
- keep layout memory per category even if sort/filter memory later becomes category-aware too
- permission-loss flows hide stored history fallbacks instead of deleting entries; stale history cleanup stays gated on a successful scan that confirms the item is gone
- expose advanced filter chips inline (currently behind a single dialog) if product wants always-on
  visibility for active filters

## Do Not Regress

- Do not merge Home category view mode back into one global preference.
- Do not remove `RECENT` fixed latest-playback ordering without explicit product intent.
- Do not hide the layout toggle just because `RECENT` hides sort controls.
