# Android TV Regression Matrix

Date: 2026-06-11

This records `P5-TV-REGRESSION-001`: the Phase 5 Android TV regression matrix. It documents the current TV / remote-control coverage and the repeatable checks to run before claiming Android TV readiness. It does not add new runtime behavior.

## Scope

The matrix covers the current implementation:

- TV launch metadata through `LEANBACK_LAUNCHER`.
- TV mode selection through `MainActivityTvModePolicy`.
- TV home shell through `TvHomeFragment`.
- Remote playback keys through `PlayerActivity`, including `KEYCODE_DPAD_CENTER`, `KEYCODE_MEDIA_PLAY_PAUSE`, and `runRemoteSeekAction(event)`.
- Existing subtitle, audio, source, and settings screens reused from the phone/tablet code path.

## Device Setup

Run the normal debug build and smoke install first:

```powershell
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
gradle :app:lintDebug --warning-mode fail
git diff --check
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.openvideo/.ui.MainActivity
adb logcat -d -v time AndroidRuntime:E *:S
```

Record the device type before TV validation:

```powershell
adb shell pm list features | Select-String -Pattern 'leanback|television|touchscreen'
adb shell cmd uimode help
adb shell dumpsys window | Select-String -Pattern 'mCurrentFocus|mFocusedApp'
```

If the connected device has no Leanback / television feature and `cmd uimode` cannot switch to television mode, mark TV-only visual traversal as "not run on this device" and keep the install/start/logcat smoke result.

## Regression Matrix

| Area | Steps | Expected result | Current code anchor |
|---|---|---|---|
| Launch and TV mode | Install on Android TV or TV emulator, launch from Leanback launcher, then launch through `adb shell am start -n com.example.openvideo/.ui.MainActivity`. | App starts without phone bottom navigation, `TvHomeFragment` is the first screen; if media permission is missing, first focus lands on the permission explanation card, otherwise it lands on Continue watching. | `AndroidManifest.xml` `LEANBACK_LAUNCHER`; `MainActivityTvModePolicy`; `MainActivity`; `TvHomeFragment` |
| D-pad focus | From TV home, press Right, Down, Left, Up across Continue watching, Folders, Series, Sources, Settings. Repeat with the permission panel visible and hidden. | Focus follows the visible card grid and stays on a card; Continue watching does not move focus to a hidden permission panel; no invisible or phone bottom-nav target receives focus. | `fragment_tv_home.xml` `nextFocus*`; `TvHomeFragment.bindPermissionPanel(...)`; `bg_focusable_card` |
| Card routing | Press OK / Enter on each TV home card. | Continue watching opens the existing Home Recent category and shows the first recent title/count summary when recent history exists; Folders, Series, Sources, and Settings route to the existing screens; Back returns through the fragment stack. | `TvHomeFragment.bindCard(...)`; `TvHomeFragment.bindContinueCover(...)`; `HomeFragment.newInstance(HomeCategory.RECENT)` |
| Playback remote keys | Open a playable local or network video, press OK / Enter / Space / media play-pause, D-pad Left / Right, media rewind / fast-forward, D-pad Up / Down, Menu, Back. | Play/pause toggles, seek uses the user interval, long press repeats by repeated key-down events, Up/Menu shows controls, Down hides controls, Back follows the existing player policy. | `PlayerActivity` `KEYCODE_DPAD_CENTER`; `KEYCODE_MEDIA_PLAY_PAUSE`; `runRemoteSeekAction(event)` |
| Subtitles and audio | While playing, use keyboard S/A or existing settings entries to open subtitle and audio choices; navigate choices with D-pad and confirm with OK. | Subtitle/audio sheets are focusable, default focus lands on a usable row, and Back dismisses the sheet without losing the player. | `PlayerGlassSheetDialog`; `PlayerSubtitleSettingsSheet`; `PlayerAudioSettingsSheet` |
| Sources | Open Sources from TV home, confirm initial focus, move focus through Local, Open URL, saved source rows, recent playback rows, WebDAV, and future-source placeholder; open URL, saved source detail, and WebDAV entry if credentials are available. | Existing source screens are reachable by remote; initial focus lands on the Local source row; Sources fixed rows, saved source rows, recent playback rows, and WebDAV browser rows have an explicit focus ring; Sources moves Down through Local, Open URL, the present saved/recent lists, WebDAV, and Future without targeting hidden empty lists; saved source detail and WebDAV browser start on Back; source detail moves from Test directly to Delete for URL sources and through Browse for WebDAV sources; if the source no longer exists, SourceDetail hides invalid actions and Down from Back enters the focusable missing-state message; WebDAV browser moves Down from Back into the entry list when entries exist or into the focusable empty state when not; dialogs have default focus; sensitive WebDAV credentials are not exposed in URLs. | `fragment_sources.xml`; `fragment_source_detail.xml`; `item_media_source.xml`; `item_source_recent_playback.xml`; `fragment_webdav_browser.xml`; `item_webdav_entry.xml`; `SourcesFragment`; `SourceDetailFragment`; `NetworkOpenUrlDialog`; `WebDavSourceDialog`; `WebDavBrowserFragment` |
| Settings | Open Settings from TV home and navigate common playback, subtitle/audio, source, storage/about rows with D-pad. | TV mode hides non-remote-first rows, default focus lands on Default aspect ratio, retained actionable rows are focusable, subtitle/audio rows open the existing player settings activities, the source row opens the existing Sources screen, destructive confirmations default to Cancel, and Back exits predictably. | `SettingsFragment`; `fragment_settings.xml`; `PlayerSubtitleSettingsActivity`; `PlayerAudioSettingsActivity`; `SourcesFragment`; `SettingsConfirmationActionSheet` |
| Crash smoke | After each path, run `adb logcat -d -v time AndroidRuntime:E *:S`. | No OpenVideo `FATAL EXCEPTION` or `AndroidRuntime` crash is emitted. | adb logcat |

## Verification Notes

- Always keep a screenshot or short note for the TV device / emulator model, Android version, and whether Leanback feature was present.
- For network playback samples, do not commit private, signed, tokenized, account-bound, or unstable URLs.
- If media permission prompts appear, record whether the TV home explanation card and the system prompt can be completed with D-pad / OK / Back.
- If the test device is a phone or tablet, do not claim TV focus traversal passed. Only claim the install/start/logcat smoke result for that device.

## Latest adb smoke

2026-06-11, device `71615e08` after the SourceDetail missing-state focus slice:

- Device class: phone-class, physical size `1080x2340`.
- Features: `android.hardware.touchscreen` only for the TV check; no Leanback or television feature was reported.
- `cmd uimode help`: no television mode switch command is available on this device.
- `adb install -r app\build\outputs\apk\debug\app-debug.apk`: success.
- `adb shell am start -W -n com.example.openvideo/.ui.MainActivity`: status `ok`, `WaitTime: 3028`.
- Focus after launch: `mFocusedApp` is `com.example.openvideo/.ui.MainActivity`; `mCurrentFocus` remained `NotificationShade` on this phone-class device, so this run is not usable for TV visual focus acceptance.
- `adb logcat -d -v time AndroidRuntime:E *:S`: no AndroidRuntime error output.
- TV-only visual traversal: not run on this device.

## Known limits

- Current TV home is an entry shell; Continue watching opens the existing Home Recent category and can show the first recent `thumbnailUri`, title, and recent count, but TV-specific aggregation, auto thumbnail extraction, and thumbnail carousel are not complete.
- Current TV settings are a basic simplification of `SettingsFragment` with retained-row default focus and subtitle/audio/source shortcuts into existing screens; a dedicated 10-foot settings redesign is not complete.
- Current TV first-run permission flow has a TV home explanation card and system permission request, but system prompt completion still needs TV hardware or emulator validation.
- Current TV home Series cover uses the first available Phase 2 series poster only; Continue watching cover uses the first recent video thumbnail only.
- Sources rows now have explicit `bg_focusable_card` focus styling, a Local-row default focus request, content-aware Sources page Down navigation across saved/recent sections, SourceDetail Back-button default focus, action focus order and missing-state focus fallback, and WebDAV browser Back-button / row / empty-state focus support with content-aware Down navigation, but true D-pad traversal through Sources still needs a TV device or TV emulator pass.
- A true Android TV device or TV emulator is required to validate Leanback launcher behavior and real D-pad traversal.
- The current connected phone-class adb device may report only touchscreen features and may keep focus on AOD / lockscreen; that is not sufficient for TV visual acceptance.
- Store screenshots, remote help artwork, and release known-limit copy remain separate Phase 5.5 follow-ups.
