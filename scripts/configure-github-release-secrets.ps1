# Upload the local Android release signing material to GitHub Actions secrets.
param(
    [string] $KeystorePath = "",
    [string] $KeyAlias = "openvideo",
    [string] $Repository = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($KeystorePath)) {
    $KeystorePath = Join-Path $repoRoot "output\openvideo-default.jks"
}
$keystoreResolved = (Resolve-Path $KeystorePath -ErrorAction Stop).Path

$gh = Get-Command "gh" -ErrorAction SilentlyContinue
$ghExecutable = if ($gh) { $gh.Source } else { "" }
if ([string]::IsNullOrWhiteSpace($ghExecutable)) {
    $knownPaths = @(
        (Join-Path $env:ProgramFiles "GitHub CLI\gh.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\GitHub CLI\gh.exe"),
        (Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links\gh.exe")
    )
    $ghPath = $knownPaths | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($ghPath) {
        $ghExecutable = $ghPath
    } else {
        throw "GitHub CLI was not found. Install it from https://cli.github.com/ and run 'gh auth login'."
    }
}

& $ghExecutable auth status
if ($LASTEXITCODE -ne 0) {
    throw "GitHub CLI is not authenticated. Run 'gh auth login' first."
}

function Read-PlainSecret([string] $Prompt) {
    $secure = Read-Host -AsSecureString $Prompt
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Set-RepositorySecret([string] $Name, [string] $Value) {
    $arguments = @("secret", "set", $Name)
    if (-not [string]::IsNullOrWhiteSpace($Repository)) {
        $arguments += @("--repo", $Repository)
    }

    $Value | & $ghExecutable @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to set GitHub Actions secret: $Name"
    }
}

$storePassword = Read-PlainSecret "Keystore password"
$keyPassword = Read-PlainSecret "Key password (press Enter to reuse keystore password)"
if ([string]::IsNullOrWhiteSpace($keyPassword)) {
    $keyPassword = $storePassword
}

$keystoreBase64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystoreResolved))
Set-RepositorySecret "OPENVIDEO_RELEASE_KEYSTORE_BASE64" $keystoreBase64
Set-RepositorySecret "OPENVIDEO_RELEASE_STORE_PASSWORD" $storePassword
Set-RepositorySecret "OPENVIDEO_RELEASE_KEY_ALIAS" $KeyAlias
Set-RepositorySecret "OPENVIDEO_RELEASE_KEY_PASSWORD" $keyPassword

Write-Host "Configured GitHub Actions release signing secrets."
Write-Host "Keystore: $keystoreResolved"
Write-Host "Alias: $KeyAlias"
