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

## Gradle 10 Follow-Up

The current baseline is Gradle 9.5 with AGP 8.7.3. Gradle reports deprecated features that will be incompatible with Gradle 10. Keep the current baseline for this P4 slice, then handle the Gradle 10 alignment in a separate upgrade pass with `--warning-mode all`, AGP compatibility checks, and a full debug/release build verification.
