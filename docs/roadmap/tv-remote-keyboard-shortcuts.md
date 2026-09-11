# Remote and Keyboard Shortcuts

Date: 2026-06-09

This records `P5-REMOTE-KEYS-001`: the Phase 5 remote and keyboard shortcut table for Android TV, tablet keyboard, and desktop-like playback. It documents the current `PlayerActivity` key map and does not add runtime behavior.

Related documents:

- [`tv-regression-matrix.md`](./tv-regression-matrix.md)
- [`tv-release-assets.md`](./tv-release-assets.md)
- [`phases/phase-5-tv-tablet-remote/README.md`](./phases/phase-5-tv-tablet-remote/README.md)

## Shortcut Table

| Input | Key constants | Current behavior | Gate |
|---|---|---|---|
| OK / Enter / Numpad Enter / Space / Media Play-Pause | `KEYCODE_DPAD_CENTER`, `KEYCODE_ENTER`, `KEYCODE_NUMPAD_ENTER`, `KEYCODE_SPACE`, `KEYCODE_MEDIA_PLAY_PAUSE` | Toggle play / pause and sync the play button icon. | `PlayerLockedInteraction.TRANSPORT` |
| D-pad Left / Media Rewind | `KEYCODE_DPAD_LEFT`, `KEYCODE_MEDIA_REWIND` | Seek backward by the user seek interval. Repeated key-down events from long press run through `runRemoteSeekAction(event)`. | `PlayerLockedInteraction.TRANSPORT` |
| D-pad Right / Media Fast-Forward | `KEYCODE_DPAD_RIGHT`, `KEYCODE_MEDIA_FAST_FORWARD` | Seek forward by the user seek interval. Repeated key-down events from long press run through `runRemoteSeekAction(event)`. | `PlayerLockedInteraction.TRANSPORT` |
| D-pad Up / Menu | `KEYCODE_DPAD_UP`, `KEYCODE_MENU` | Show playback controls. | `PlayerLockedInteraction.CHROME_TOGGLE` |
| D-pad Down | `KEYCODE_DPAD_DOWN` | Hide playback controls. | `PlayerLockedInteraction.CHROME_TOGGLE` |
| Back | Android Back dispatcher | When locked, show locked controls. When controls are hidden, show controls first. When controls are visible, exit playback through the existing player Back policy. | Player Back policy |
| J / K / L | `KEYCODE_J`, `KEYCODE_K`, `KEYCODE_L` | J seeks backward, K toggles play / pause, L seeks forward. | `playerPrefs.keyboardShortcuts`, then `PlayerLockedInteraction.TRANSPORT` |
| S / A | `KEYCODE_S`, `KEYCODE_A` | S opens the subtitle quick dialog. A opens the audio quick dialog. | `playerPrefs.keyboardShortcuts`, then `PlayerLockedInteraction.SETTINGS` |

## Behavior Boundaries

- Remote transport keys do not depend on the keyboard shortcut preference.
- Letter shortcuts are disabled when `playerPrefs.keyboardShortcuts` is false.
- Locked playback does not execute transport, settings, or chrome actions directly. It shows locked controls instead.
- Long-press seek does not implement custom acceleration; Android repeated key-down events each apply one user seek interval.
- Subtitle and audio shortcuts open the existing quick dialogs; they do not cycle tracks directly.

## Code Anchors

- `PlayerActivity.onKeyDown(...)`: central key dispatch.
- `runRemoteTransportAction(...)`: play / pause and seek lock gate.
- `runRemoteSeekAction(event)`: D-pad and media seek repeat handling.
- `runRemoteChromeVisibilityAction(...)`: controls show / hide lock gate.
- `runKeyboardShortcutAction(...)`: J / K / L preference gate.
- `runKeyboardSettingsShortcutAction(...)`: S / A preference and settings gate.
- `PlayerRemoteKeySourceTest`: source-level regression coverage for the key map.

## Verification Notes

- The current phone-class adb smoke can verify install, launch, and crash-free startup only.
- True D-pad traversal and long-press behavior still need an Android TV device or TV emulator pass recorded in `tv-regression-matrix.md`.
- Keep this table aligned with `tv-release-assets.md` remote help artwork requirements.
