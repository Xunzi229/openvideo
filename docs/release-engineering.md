# Release Engineering

This project keeps release metadata and local automation in the repository, while secrets and generated packages stay local.

## Tracked Release Files

- `scripts/` is tracked so local packaging and signing helpers are reviewable.
- `LICENSE` declares the repository license.

## Versioning

Android `versionCode` and `versionName` are read from `gradle.properties`:

- `VERSION_CODE`
- `VERSION_NAME`

Update both before cutting a release.

## Signing

Release signing is optional for local builds. It is enabled only when all of these values are provided through environment variables or `local.properties`:

- `OPENVIDEO_RELEASE_STORE_FILE`
- `OPENVIDEO_RELEASE_STORE_PASSWORD`
- `OPENVIDEO_RELEASE_KEY_ALIAS`
- `OPENVIDEO_RELEASE_KEY_PASSWORD`

Keystores and signing material must not be committed.

The local wrapper uses `output/openvideo-default.jks` and alias `openvideo` by default, but passwords are read interactively or from the same `OPENVIDEO_RELEASE_*` environment variables. Passwords are never stored in the script.

### Signing Consistency

GitHub Actions Preview artifacts and published Release artifacts are both built with the release
keystore. Both workflows verify the APK certificate against the public
`OPENVIDEO_RELEASE_CERT_SHA256` repository variable before upload. Preview builds do not create a
tag or GitHub Release; "preview" describes distribution status, not a different signing identity.

`assembleRelease` locally stays a single APK. GitHub Actions Preview and Release pass
`-POPENVIDEO_ABI_SPLITS=true` so those jobs produce ABI-split APKs plus a universal APK:

- `armeabi-v7a`
- `arm64-v8a`
- `x86`
- `x86_64`
- `universal`

Preview uploads those files as Actions artifacts. Download them with `gh run download <run-id>`
and do not use `gh release create` for preview.

When release signing values are present locally, Gradle also signs Debug builds with the release
keystore so they can update an installed release build. Without those values, Debug builds retain
Android's development key and must not be distributed as Preview artifacts.

## GitHub Actions Release

The `Release APK` workflow restores the existing keystore from encrypted GitHub Actions secrets, builds the signed release APK, verifies its certificate, writes `SHA256SUMS.txt`, and publishes both files to a GitHub Release.

Configure the repository secrets once from the repository root:

```powershell
.\scripts\configure-github-release-secrets.ps1 `
  -KeystorePath .\output\openvideo-default.jks `
  -KeyAlias openvideo `
  -Repository Xunzi229/openvideo
```

The helper prompts for the keystore and key passwords without echoing them, then sets these repository secrets:

- `OPENVIDEO_RELEASE_KEYSTORE_BASE64`
- `OPENVIDEO_RELEASE_STORE_PASSWORD`
- `OPENVIDEO_RELEASE_KEY_ALIAS`
- `OPENVIDEO_RELEASE_KEY_PASSWORD`

The keystore Base64 value is a transport encoding, not a password or encryption mechanism. GitHub stores it as an encrypted Actions secret, and the workflow restores it only under the runner's temporary directory.

To publish, first update `VERSION_NAME` and `VERSION_CODE` in `gradle.properties`, commit the change, then push a matching tag:

```powershell
git tag v0.0.16
git push origin v0.0.16
```

The tag must exactly equal `v` plus `VERSION_NAME`. The workflow can also be started manually with the same tag value from the Actions page. Manual runs leave `publish_release` disabled by default, producing a signed artifact without creating a tag or GitHub Release; enable it explicitly when a manual run should publish.

## Packaging

`scripts/OpenVideo.Release.psm1` is the shared module for version resolution, artifact renaming, checksums, and release notes. `scripts/package-helper.ps1` and `scripts/sign-release.ps1` import it so naming stays aligned with `gradle.properties`.

`scripts/package-helper.ps1` collects package artifacts and writes:

- `RELEASE_NOTES.md`
- `SHA256SUMS.txt`

Run `scripts/tests/Test-OpenVideoRelease.ps1` before release packaging to guard version parsing and release artifact formatting.

### Feature packaging flags

The phone bottom-navigation Sources entry is disabled by default while the feature is incomplete. Set `OPENVIDEO_SOURCES_NAV_ENABLED=true` as an environment variable, a Gradle project property (`-POPENVIDEO_SOURCES_NAV_ENABLED=true`), or a `local.properties` value to include the entry in a local build.

GitHub Actions reads the repository variable with the same name. Leave it unset or set it to `false` to hide Sources; set it to `true` to show Sources in Preview and Release APKs. This flag only controls the phone bottom-navigation entry and does not remove the Sources implementation.

## Local Verification

GitHub workflow auto checks are intentionally not used. Run the verification commands locally before packaging or pushing release changes.

## Gradle 10 Compatibility

The current baseline is Gradle 9.5 with AGP 9.0.1, Kotlin 2.2.10, KSP 2.3.7, Room 2.8.3, and Dagger/Hilt 2.59.1. AGP was upgraded from 8.7.3 after `--warning-mode all` showed Gradle 10 multi-string dependency notation deprecations from AGP's internal `lint-gradle` and `aapt2` dependencies. KSP was upgraded to the 2.3 line so AGP 9 built-in Kotlin source set integration handles generated sources without the temporary bridge property. Re-run debug, lint, and release packaging with `--warning-mode all` when changing Gradle or AGP again.

### Upgrade Notes

- AGP 9 provides built-in Kotlin support, so the project no longer applies `org.jetbrains.kotlin.android` directly.
- Hilt was upgraded to 2.59.1 because older versions depended on AGP 8 `BaseExtension` APIs.
- Room was upgraded to 2.8.3 because Room 2.6.1 failed under Kotlin 2.2 / KSP 2 with `unexpected jvm signature V`.
- Hilt `@ApplicationContext` constructor parameters use explicit `@param:` targets to avoid Kotlin 2.2 annotation target drift.
- KSP generated sources are covered by the normal AGP 9 built-in Kotlin source set integration.

### Verification Commands

Use these commands after any Gradle, AGP, Kotlin, KSP, Hilt, or Room version change:

```bash
./gradlew :app:testDebugUnitTest --warning-mode fail
./gradlew :app:assembleDebug --warning-mode fail
./gradlew :app:lintDebug --warning-mode fail
```

For release packaging on Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/sign-release-default.ps1
```
