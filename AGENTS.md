# OpenVideo agent notes

## APK signing

GitHub Actions Preview and Release APKs must use the same official release keystore from repository secrets (`OPENVIDEO_RELEASE_*`). Never package `assembleDebug` for distribution. Preview must not create a GitHub Release, but its signature must match Release so users can install over an existing build.

See `.cursor/rules/apk-release-signing.mdc`.
