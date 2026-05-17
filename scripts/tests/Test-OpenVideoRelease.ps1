# Wave 4 release-engineering smoke tests (no Pester dependency).
$ErrorActionPreference = "Stop"

$scriptsRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $scriptsRoot
Import-Module (Join-Path $scriptsRoot "OpenVideo.Release.psm1") -Force

$failures = 0

function Assert-True([bool] $condition, [string] $message) {
    if (-not $condition) {
        Write-Host "FAIL: $message"
        $script:failures++
    } else {
        Write-Host "OK: $message"
    }
}

function Assert-Equal($expected, $actual, [string] $message) {
    if ("$expected" -ne "$actual") {
        Write-Host "FAIL: $message (expected '$expected', got '$actual')"
        $script:failures++
    } else {
        Write-Host "OK: $message"
    }
}

$version = Get-OpenVideoProjectVersion -RepoRoot $repoRoot
Assert-Equal "0.0.8" $version.Name "VERSION_NAME from gradle.properties"
Assert-Equal 8 $version.Code "VERSION_CODE from gradle.properties"

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) ("openvideo-release-test-" + [guid]::NewGuid().ToString("n"))
New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
try {
    $metaDir = Join-Path $tempRoot "meta"
    New-Item -ItemType Directory -Path $metaDir -Force | Out-Null
  $metaJson = @'
{
  "elements": [
    { "outputFile": "app-release-unsigned.apk", "versionName": "9.9.9" }
  ]
}
'@
    Set-Content -Path (Join-Path $metaDir "output-metadata.json") -Value $metaJson -Encoding UTF8
    $artifactPath = Join-Path $metaDir "app-release-unsigned.apk"
    New-Item -ItemType File -Path $artifactPath -Force | Out-Null
    Assert-Equal "9.9.9" (Resolve-OpenVideoVersionName -RepoRoot $repoRoot -ArtifactPath $artifactPath) "metadata versionName wins"

    $collectDir = Join-Path $tempRoot "collect"
    New-Item -ItemType Directory -Path $collectDir -Force | Out-Null
    $sample = Join-Path $collectDir "openvideo-v0.0.8-app-debug.apk"
    Set-Content -Path $sample -Value "sample" -Encoding UTF8

    Write-OpenVideoReleaseChecksums -CollectDir $collectDir | Out-Null
    $checksumFile = Join-Path $collectDir "SHA256SUMS.txt"
    Assert-True (Test-Path $checksumFile) "SHA256SUMS.txt is created"
    $checksumLine = Get-Content $checksumFile -TotalCount 1
    Assert-True ($checksumLine -match '^[a-f0-9]{64}  openvideo-v0\.0\.8-app-debug\.apk$') "checksum line format"

    Write-OpenVideoReleaseNotes -CollectDir $collectDir -VersionName "0.0.8" | Out-Null
    $notesFile = Join-Path $collectDir "RELEASE_NOTES.md"
    Assert-True (Test-Path $notesFile) "RELEASE_NOTES.md is created"
    $notes = Get-Content $notesFile -Raw
    Assert-True ($notes -match '# OpenVideo v0\.0\.8') "release notes title"
    Assert-True ($notes -match 'SHA256SUMS\.txt') "release notes mention checksums"

    $renamed = Get-OpenVideoArtifactFileName -VersionName "0.0.8" -SourceFileName "app-debug.apk"
    Assert-Equal "openvideo-v0.0.8-app-debug.apk" $renamed "artifact rename pattern"
} finally {
    Remove-Item -Path $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}

if ($failures -gt 0) {
    Write-Host ""
    Write-Host "$failures test(s) failed."
    exit 1
}

Write-Host ""
Write-Host "All OpenVideo release script tests passed."
