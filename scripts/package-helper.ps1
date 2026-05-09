# One command: build + collect artifacts + open output directory.
param(
    [string] $OutputBaseDir = "",
    [switch] $SkipBuild,
    [switch] $ReleaseOnly,
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

function Resolve-VersionNameForArtifact([string] $repoRoot, $artifact) {
    $metadataPath = Join-Path $artifact.Directory.FullName "output-metadata.json"
    if (Test-Path $metadataPath) {
        try {
            $meta = Get-Content $metadataPath -Raw | ConvertFrom-Json
            if ($meta.elements) {
                $match = $meta.elements | Where-Object { $_.outputFile -eq $artifact.Name } | Select-Object -First 1
                if ($match -and $match.versionName) {
                    return [string]$match.versionName
                }
                $first = $meta.elements | Select-Object -First 1
                if ($first -and $first.versionName) {
                    return [string]$first.versionName
                }
            }
        } catch {
            # ignore metadata parse errors and fallback
        }
    }

    $buildGradle = Join-Path $repoRoot "app\build.gradle.kts"
    if (Test-Path $buildGradle) {
        $content = Get-Content $buildGradle -Raw
        $m = [regex]::Match($content, 'versionName\s*=\s*"([^"]+)"')
        if ($m.Success) {
            return $m.Groups[1].Value
        }
    }
    return "unknown"
}

if (-not $SkipBuild) {
    if ($ReleaseOnly) {
        & ".\gradlew.bat" "assembleRelease"
    } else {
        & ".\gradlew.bat" "assembleDebug" "assembleRelease"
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$collectDir = Join-Path $OutputBaseDir "upload-packages-$timestamp"
New-Item -ItemType Directory -Path $collectDir -Force | Out-Null

$artifacts = Get-ChildItem (Join-Path $repoRoot "app\build\outputs") -Recurse -File |
    Where-Object {
        $_.Extension -in ".apk", ".aab", ".apks" -and (
            -not $ReleaseOnly -or
            $_.FullName -match "\\release\\"
        )
    }

if (-not $artifacts) {
    throw "No package artifacts found under app/build/outputs."
}

foreach ($artifact in $artifacts) {
    $versionName = Resolve-VersionNameForArtifact $repoRoot $artifact
    $base = [IO.Path]::GetFileNameWithoutExtension($artifact.Name)
    $ext = [IO.Path]::GetExtension($artifact.Name)
    $targetName = "openvideo-v{0}-{1}{2}" -f $versionName, $base, $ext
    $targetPath = Join-Path $collectDir $targetName
    Copy-Item $artifact.FullName -Destination $targetPath -Force
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
