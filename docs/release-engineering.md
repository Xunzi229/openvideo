# Release Engineering

This project keeps release metadata and local automation in the repository, while secrets and generated packages stay local.

## Tracked Release Files

- `.github/` is tracked so CI changes are reviewable.
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

## Packaging

`scripts/package-helper.ps1` collects package artifacts and writes:

- `RELEASE_NOTES.md`
- `SHA256SUMS.txt`

## Gradle 10 Compatibility

The current baseline is Gradle 9.5 with AGP 9.0.1, Kotlin 2.2.10, KSP 2.2.10-2.0.2, Room 2.8.3, and Dagger/Hilt 2.59.1. AGP was upgraded from 8.7.3 after `--warning-mode all` showed Gradle 10 multi-string dependency notation deprecations from AGP's internal `lint-gradle` and `aapt2` dependencies. `android.disallowKotlinSourceSets=false` is kept as a temporary KSP generated-source compatibility bridge while KSP's AGP 9 built-in Kotlin integration settles. Re-run debug, lint, and release packaging with `--warning-mode all` when changing Gradle or AGP again.

### Upgrade Notes

- AGP 9 provides built-in Kotlin support, so the project no longer applies `org.jetbrains.kotlin.android` directly.
- Hilt was upgraded to 2.59.1 because older versions depended on AGP 8 `BaseExtension` APIs.
- Room was upgraded to 2.8.3 because Room 2.6.1 failed under Kotlin 2.2 / KSP 2 with `unexpected jvm signature V`.
- Hilt `@ApplicationContext` constructor parameters use explicit `@param:` targets to avoid Kotlin 2.2 annotation target drift.
- The temporary `android.disallowKotlinSourceSets=false` switch should be revisited when KSP no longer needs the generated-source compatibility bridge.

### Verification Commands

Use these commands after any Gradle, AGP, Kotlin, KSP, Hilt, or Room version change:

```bash
./gradlew :app:testDebugUnitTest --warning-mode all
./gradlew :app:assembleDebug --warning-mode all
./gradlew :app:lintDebug --warning-mode all
./gradlew :app:assembleDebug --warning-mode fail
```

For release packaging on Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/sign-release-default.ps1
```
