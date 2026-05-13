# Default local signing wrapper (uses output/openvideo-default.jks).
param(
    [string] $UnsignedApkPath = "",
    [string] $OutputApkPath = "",
    [switch] $SkipBuild,
    [switch] $NoOpenDir
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$defaultKeystore = Join-Path $repoRoot "output\openvideo-default.jks"
if (-not (Test-Path $defaultKeystore)) {
    throw "Default keystore not found: $defaultKeystore"
}

if (-not $SkipBuild -and [string]::IsNullOrWhiteSpace($UnsignedApkPath)) {
    & ".\gradlew.bat" "assembleRelease"
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle release build failed with exit code $LASTEXITCODE"
    }
}

$signArgs = @{
    KeystorePath  = $defaultKeystore
    KeyAlias      = "openvideo"
    StorePassword = "openvideo123"
    KeyPassword   = "openvideo123"
    NoOpenDir     = $NoOpenDir
}
if (-not [string]::IsNullOrWhiteSpace($UnsignedApkPath)) {
    $signArgs.UnsignedApkPath = $UnsignedApkPath
}
if (-not [string]::IsNullOrWhiteSpace($OutputApkPath)) {
    $signArgs.OutputApkPath = $OutputApkPath
}

& (Join-Path $PSScriptRoot "sign-release.ps1") @signArgs

if ($LASTEXITCODE -ne 0) {
    throw "Default sign failed with exit code $LASTEXITCODE"
}
