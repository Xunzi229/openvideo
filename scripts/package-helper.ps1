# One command: build + collect artifacts + open output directory.
param(
    [string] $OutputBaseDir = "",
    [switch] $SkipBuild,
    [switch] $NoOpenDir
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if ([string]::IsNullOrWhiteSpace($OutputBaseDir)) {
    $OutputBaseDir = Join-Path $repoRoot "output"
}

if (-not (Test-Path $OutputBaseDir)) {
    New-Item -ItemType Directory -Path $OutputBaseDir -Force | Out-Null
}

if (-not $SkipBuild) {
    & ".\gradlew.bat" assembleDebug assembleRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$collectDir = Join-Path $OutputBaseDir "upload-packages-$timestamp"
New-Item -ItemType Directory -Path $collectDir -Force | Out-Null

$artifacts = Get-ChildItem (Join-Path $repoRoot "app\build\outputs") -Recurse -File |
    Where-Object { $_.Extension -in ".apk", ".aab", ".apks" }

if (-not $artifacts) {
    throw "No package artifacts found under app/build/outputs."
}

foreach ($artifact in $artifacts) {
    Copy-Item $artifact.FullName -Destination $collectDir -Force
}

Write-Host "Done. Upload directory:"
Write-Host $collectDir
Write-Host "Files:"
Get-ChildItem $collectDir -File | ForEach-Object { Write-Host "- $($_.Name)" }

if (-not $NoOpenDir) {
    Start-Process "explorer.exe" $collectDir
}

# Easy to parse in scripts/CI logs
Write-Output "DEST=$collectDir"
