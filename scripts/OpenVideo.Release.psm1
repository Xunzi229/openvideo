# Shared release helpers: version resolution, checksums, release notes.
# Single source of truth for VERSION_* is gradle.properties (see docs/release-engineering.md).

function Get-OpenVideoProjectVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string] $RepoRoot
    )

    $gradleProperties = Join-Path $RepoRoot "gradle.properties"
    if (-not (Test-Path $gradleProperties)) {
        throw "Missing gradle.properties at $gradleProperties"
    }

    $content = Get-Content $gradleProperties -Raw
    $nameMatch = [regex]::Match($content, '(?m)^VERSION_NAME\s*=\s*(.+?)\s*$')
    $codeMatch = [regex]::Match($content, '(?m)^VERSION_CODE\s*=\s*(\d+)\s*$')
    if (-not $nameMatch.Success -or -not $codeMatch.Success) {
        throw "gradle.properties must define VERSION_NAME and VERSION_CODE."
    }

    return [PSCustomObject]@{
        Name = $nameMatch.Groups[1].Value.Trim()
        Code = [int]$codeMatch.Groups[1].Value
    }
}

function Resolve-OpenVideoVersionName {
    param(
        [Parameter(Mandatory = $true)]
        [string] $RepoRoot,
        [string] $ArtifactPath = ""
    )

    if (-not [string]::IsNullOrWhiteSpace($ArtifactPath)) {
        $metadataPath = Join-Path (Split-Path -Parent $ArtifactPath) "output-metadata.json"
        if (Test-Path $metadataPath) {
            try {
                $meta = Get-Content $metadataPath -Raw | ConvertFrom-Json
                if ($meta.elements) {
                    $artifactName = Split-Path -Leaf $ArtifactPath
                    $match = $meta.elements | Where-Object { $_.outputFile -eq $artifactName } | Select-Object -First 1
                    if ($match -and $match.versionName) {
                        return [string]$match.versionName
                    }
                    $first = $meta.elements | Select-Object -First 1
                    if ($first -and $first.versionName) {
                        return [string]$first.versionName
                    }
                }
            } catch {
                Write-Warning "Cannot parse output-metadata.json at $metadataPath"
            }
        }
    }

    return (Get-OpenVideoProjectVersion -RepoRoot $RepoRoot).Name
}

function Get-OpenVideoArtifactFileName {
    param(
        [Parameter(Mandatory = $true)]
        [string] $VersionName,
        [Parameter(Mandatory = $true)]
        [string] $SourceFileName
    )

    $base = [IO.Path]::GetFileNameWithoutExtension($SourceFileName)
    $ext = [IO.Path]::GetExtension($SourceFileName)
    return "openvideo-v{0}-{1}{2}" -f $VersionName, $base, $ext
}

function Write-OpenVideoReleaseChecksums {
    param(
        [Parameter(Mandatory = $true)]
        [string] $CollectDir
    )

    $checksumPath = Join-Path $CollectDir "SHA256SUMS.txt"
    $lines = Get-ChildItem $CollectDir -File |
        Where-Object { $_.Name -ne "SHA256SUMS.txt" -and $_.Name -ne "RELEASE_NOTES.md" } |
        Sort-Object Name |
        ForEach-Object {
            $hash = Get-FileHash -Algorithm SHA256 -Path $_.FullName
            "{0}  {1}" -f $hash.Hash.ToLowerInvariant(), $_.Name
        }
    $lines | Set-Content -Path $checksumPath -Encoding UTF8
    return $checksumPath
}

function Write-OpenVideoReleaseNotes {
    param(
        [Parameter(Mandatory = $true)]
        [string] $CollectDir,
        [Parameter(Mandatory = $true)]
        [string] $VersionName
    )

    $notesPath = Join-Path $CollectDir "RELEASE_NOTES.md"
    $files = Get-ChildItem $CollectDir -File |
        Where-Object { $_.Name -ne "RELEASE_NOTES.md" } |
        Sort-Object Name |
        ForEach-Object { "- $($_.Name)" }

    $notes = @(
        "# OpenVideo v$VersionName",
        "",
        "Build date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
        "",
        "## Artifacts",
        $files,
        "",
        "## Verification",
        "- SHA-256 checksums are listed in SHA256SUMS.txt."
    )
    $notes | Set-Content -Path $notesPath -Encoding UTF8
    return $notesPath
}

Export-ModuleMember -Function @(
    'Get-OpenVideoProjectVersion',
    'Resolve-OpenVideoVersionName',
    'Get-OpenVideoArtifactFileName',
    'Write-OpenVideoReleaseChecksums',
    'Write-OpenVideoReleaseNotes'
)
