# Network Source Privacy

OpenVideo stores network source metadata separately from credentials.

## Stored Metadata

- URL sources store type, name, address, display address, normalized address, enabled state, and timestamps in `media_sources`.
- Display addresses redact sensitive query values such as token, key, sign, and auth.
- The current URL source flow does not store usernames, passwords, cookies, Authorization headers, or arbitrary request headers.

## Credential Storage

- WebDAV credentials are not implemented in the current source detail slice.
- When WebDAV credentials are added, passwords and tokens must be stored with Android Keystore backed encrypted storage such as `EncryptedSharedPreferences`.
- Plain Room tables, plain SharedPreferences, crash logs, copied diagnostics, and release feedback must not contain passwords, tokens, cookies, or Authorization headers.

## Settings Export

- Settings export remains a non-sensitive settings export.
- Settings export may include source names and addresses only after an explicit source export flow exists.
- Settings export must exclude passwords, tokens, cookies, and headers.
- Existing `SettingsBackupAllowlistPolicy` blocks sensitive markers including password, token, cookie, and authorization.

## Diagnostics

- Diagnostics and crash reports must redact full URLs, usernames, passwords, tokens, cookies, and headers.
- Error messages should explain credential or permission failures without echoing secret values.

## Deletion

- Deleting a source must delete its metadata.
- When credential storage is implemented, deleting a source must also delete credential aliases and cached credentials for that source.
