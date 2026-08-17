# Default local signing wrapper (uses output/openvideo-default.jks by default).
param(
    [string] $UnsignedApkPath = "",
    [string] $OutputApkPath = "",
    [string] $KeystorePath = "",
    [string] $KeyAlias = "",
    [string] $StorePassword = "",
    [string] $KeyPassword = "",
    [switch] $SkipBuild,
    [switch] $NoOpenDir
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$defaultKeystore = if (-not [string]::IsNullOrWhiteSpace($KeystorePath)) {
    $KeystorePath
} elseif ($env:OPENVIDEO_RELEASE_STORE_FILE) {
    $env:OPENVIDEO_RELEASE_STORE_FILE
} else {
    Join-Path $repoRoot "output\openvideo-default.jks"
}
if (-not (Test-Path $defaultKeystore)) {
    throw "Default keystore not found: $defaultKeystore"
}

if ([string]::IsNullOrWhiteSpace($KeyAlias)) {
    $KeyAlias = if ($env:OPENVIDEO_RELEASE_KEY_ALIAS) { $env:OPENVIDEO_RELEASE_KEY_ALIAS } else { "openvideo" }
}
if ([string]::IsNullOrWhiteSpace($StorePassword) -and $env:OPENVIDEO_RELEASE_STORE_PASSWORD) {
    $StorePassword = $env:OPENVIDEO_RELEASE_STORE_PASSWORD
}
if ([string]::IsNullOrWhiteSpace($KeyPassword) -and $env:OPENVIDEO_RELEASE_KEY_PASSWORD) {
    $KeyPassword = $env:OPENVIDEO_RELEASE_KEY_PASSWORD
}

if (-not $SkipBuild -and [string]::IsNullOrWhiteSpace($UnsignedApkPath)) {
    & ".\gradlew.bat" "assembleRelease"
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle release build failed with exit code $LASTEXITCODE"
    }
}

$signArgs = @{
    KeystorePath  = $defaultKeystore
    KeyAlias      = $KeyAlias
    NoOpenDir     = $NoOpenDir
}
if (-not [string]::IsNullOrWhiteSpace($StorePassword)) {
    $signArgs.StorePassword = $StorePassword
}
if (-not [string]::IsNullOrWhiteSpace($KeyPassword)) {
    $signArgs.KeyPassword = $KeyPassword
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
