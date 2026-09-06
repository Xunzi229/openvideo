# Tests and Coverage

## Application Identity

The code package and Gradle namespace are `com.openvideo.app`; the published
application ID remains `com.example.openvideo`. Keep this explicit application
ID, the official signing certificate, database names, preference keys and
playback action strings stable when reorganizing source code.

Manifest activity aliases retain the old launcher and activity component names.
The existing `adb shell am start -n com.example.openvideo/.ui.MainActivity`
command still targets the launcher alias. `PackageIdentityCompatibilityTest`
checks the compiled identity, intent resolution, aliases and XML view inflation.
An in-place upgrade still needs verification on a device before distribution.

## Run Locally

Use JDK 17 and Android SDK Platform 36 / Build Tools 36.0.0. Set `JAVA_HOME`
and configure the SDK through `local.properties` or `ANDROID_HOME`.
Robolectric downloads its Android 28 runtime on the first run. No emulator or
device is needed for these local unit tests.

From the repository root on Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:createDebugUnitTestCoverageReport
```

The coverage task runs the full unit test suite and produces:

- Test results: `app/build/reports/tests/testDebugUnitTest/index.html`
- Coverage: `app/build/reports/coverage/test/debug/index.html`
- Coverage XML: `app/build/reports/coverage/test/debug/report.xml`

For a focused iteration:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*MediaIdentityDaoTest' --tests '*VttParserTest'
```

Run the full coverage task again before comparing totals. A filtered test run
replaces the execution data with coverage from only those tests.

## Business Coverage Gate

The foundation business scope is fixed in `coverage/business-scope.json`.
It includes policies, parsers, matchers, preferences, subtitle/network data
loading, file codecs, state models, and media identity upsert logic. It excludes
screen rendering, lifecycle controllers, repository orchestration, generated
database implementations, and other generated sources. Policy helpers located
under UI packages remain included. Patterns use PowerShell wildcard semantics
(`*` can match directory separators); new matching sources enter the denominator
automatically, even without tests.

After the full coverage task, run the 90% gate from the repository root:

```powershell
.\scripts\Test-BusinessCoverage.ps1
```

The script sums JaCoCo source-file `METHOD`, `BRANCH`, and `LINE` counters for
the same source set. Each aggregate must reach 90%, using unrounded values.
Empty counters, absent XML, and selected sources missing from the report fail
the check. No low-coverage file is removed based on its score. This does not
require each individual file to reach 90%.

Results are written to `app/build/reports/coverage/business/summary.md` and
`summary.json`; the JSON includes all 210 selected files and their counters.
`-ReportOnly` prints a diagnostic report without enforcing the threshold.

September 6, 2026 baseline and verified results for this unchanged scope:

| Metric | Before | After |
| --- | ---: | ---: |
| Method | 84.44% | 90.72% |
| Branch | 74.59% | 90.37% |
| Line | 86.39% | 94.98% |

The full suite has 1,548 passing tests, including 65 additional tests in the
coverage pass and six package identity compatibility tests. This gate measures
foundation business logic, not all app code.

## Full App Report

JaCoCo reports `METHOD` (including JVM constructors/accessors), `BRANCH`, and
`LINE` coverage. The app report includes Android screens and generated code;
it is not a percentage for pure business functions alone. Compare the same
scope and inspect individual classes for missed conditions.

The September 2026 additions exercise:

- Media identity upsert branches, match priorities, conflicting candidates,
  path normalization, and fingerprint/timestamp boundaries.
- SRT, VTT, and ASS parsing, malformed cues, encoding detection, and languages.
- Real preference reads/writes under Robolectric, legacy keys, resets,
  clamping, backup round trips, nullable fields, and sensitive value exclusion.
- Duration/date/format filtering, VideoItem overloads, and playback switches.
- WebDAV responses, request errors, cache hits, subtitle file/URI/network
  loading, queue persistence and malformed binary records, playlist import,
  password format rejection, audio diagnostics, crop geometry, and episode
  object conversions. HTTP transport tests use an in-process OkHttp interceptor
  and never call public servers.

`MediaIdentityDaoTest` executes the DAO default method against an in-memory
test double. It does not verify Room-generated SQL, SQLite transactions,
concurrent scans, or database migrations. Robolectric preference tests also do
not replace device playback and upgrade tests.

Source-string tests check wiring only and do not execute the target methods.
New behavior tests should assert observable results and cover success, empty
input, invalid input, boundaries, and relevant failure paths. Do not add
assertions solely to exercise generated data-class methods.

## CI

The Preview workflow runs the full unit suite and coverage task, enforces all
three business metrics at 90%, and uploads test results plus coverage counters.
The Markdown summary is attached to the Actions job. Source HTML is not uploaded
because generated sources can contain build-time configuration values.
APK packaging continues to use the official release signing key.

Coverage configuration follows the [Android Gradle coverage tasks](https://developer.android.com/studio/test/coverage-report).
Robolectric class loading requires JaCoCo's `includeNoLocationClasses`; JDK
internal classes are excluded from instrumentation, not application classes.
