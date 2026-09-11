# Android TV Release Assets

Date: 2026-06-09

This records `P5-TV-ASSETS-001`: the Phase 5 Android TV release asset checklist baseline. It documents the current TV banner/icon/screenshot/remote-help requirements and code anchors. It does not add runtime behavior and does not provide final store artwork.

Related documents:

- [`tv-regression-matrix.md`](./tv-regression-matrix.md)
- [`android-tv-known-limits.md`](./android-tv-known-limits.md)
- [`phases/phase-5-tv-tablet-remote/README.md`](./phases/phase-5-tv-tablet-remote/README.md)

## Summary

OpenVideo currently has Android TV launch metadata and a basic TV banner placeholder. Final Android TV store readiness still needs real-device screenshot capture, remote help artwork, and brand review. The current baseline is enough to track requirements and avoid mixing asset work into TV runtime code.

## TV banner

Current status: baseline placeholder exists.

- Manifest anchor: `android:banner="@drawable/bg_tv_banner"`.
- Drawable anchor: `bg_tv_banner`.
- Current size: 320dp x 180dp.
- Current artwork uses the existing launcher foreground centered on the app background.
- Before a store submission, review the banner on a real Android TV launcher or emulator home screen.

## TV icon

Current status: app icon is reused.

- Manifest anchor: `android:icon="@mipmap/ic_launcher"`.
- No TV-specific adaptive icon has been added in this slice.
- Phase 6 brand work can decide whether the launcher icon remains shared or gets TV-specific export guidance.

## Store screenshots

Current status: pending.

- These are not final store screenshots.
- Capture only after `tv-regression-matrix.md` has at least one Android TV device or TV emulator pass.
- Minimum screenshot set should show TV home, local/media-source browsing, playback controls, subtitle/audio selection, and settings.
- Do not use phone-class adb screenshots as TV store evidence.

## Remote help artwork

Current status: pending.

- Required controls to illustrate: OK / Enter play-pause, D-pad Left / Right seek, long-press repeated seek, D-pad Up / Menu show controls, D-pad Down hide controls, Back policy, S/A keyboard subtitle/audio shortcuts where relevant.
- Artwork should be derived from the current `PlayerActivity` remote-key behavior and the TV regression matrix, not from a separate control model.
- Keep help artwork free of private library names, WebDAV URLs, credentials, or tokenized sample links.

## Current code anchors

- `AndroidManifest.xml`: `android:banner="@drawable/bg_tv_banner"`, `android.intent.category.LEANBACK_LAUNCHER`, optional Leanback and touchscreen features.
- `bg_tv_banner.xml`: placeholder 320dp x 180dp banner.
- `TvHomeFragment`: TV home entry shell for screenshot planning.
- `PlayerActivity`: remote key behavior for help artwork planning.
- `tv-regression-matrix.md`: device validation and adb smoke baseline.
- `android-tv-known-limits.md`: release-facing limitations to keep aligned with store text.

## Release gate

Do not call Android TV release assets complete until:

- A real Android TV device or TV emulator pass is recorded in `tv-regression-matrix.md`.
- The TV banner is visually reviewed outside the phone-class adb smoke path.
- Store screenshots are captured from a TV-class environment.
- Remote help artwork reflects the implemented key paths.
- Known limits from `android-tv-known-limits.md` are reviewed against the final release copy.
