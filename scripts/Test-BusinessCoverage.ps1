param(
    [string]$Report = "app/build/reports/coverage/test/debug/report.xml",
    [string]$Scope = "coverage/business-scope.json",
    [switch]$ReportOnly
)

$ErrorActionPreference = "Stop"
$repo = Split-Path $PSScriptRoot -Parent
$configuration = Get-Content (Join-Path $repo $Scope) -Raw | ConvertFrom-Json
$settings = [System.Xml.XmlReaderSettings]::new()
$settings.DtdProcessing = [System.Xml.DtdProcessing]::Ignore
$settings.XmlResolver = $null
$reader = [System.Xml.XmlReader]::Create((Join-Path $repo $Report), $settings)
try {
    $document = [System.Xml.XmlDocument]::new()
    $document.Load($reader)
} finally {
    $reader.Dispose()
}

$prefix = "com/example/openvideo/"
$sourceRoot = Join-Path $repo "app/src/main/java/com/example/openvideo"
$selected = @{}
foreach ($file in Get-ChildItem $sourceRoot -Recurse -Filter *.kt) {
    $path = $file.FullName.Substring($sourceRoot.Length + 1).Replace('\', '/')
    if (@($configuration.includes | Where-Object { $path -like $_ }).Count -gt 0) {
        $selected[$path] = $false
    }
}
if ($selected.Count -eq 0) { throw "The business coverage scope selected no sources." }

$types = @("METHOD", "BRANCH", "LINE")
$totals = @{}
foreach ($type in $types) { $totals[$type] = @{ covered = 0; missed = 0 } }
$rows = @(
    foreach ($package in $document.report.package) {
        if (-not $package.name.StartsWith($prefix)) { continue }
        foreach ($source in $package.sourcefile) {
            $path = $package.name.Substring($prefix.Length) + "/" + $source.name
            if (-not $selected.ContainsKey($path)) { continue }
            $selected[$path] = $true
            $row = [ordered]@{ source = $path }
            foreach ($type in $types) {
                $counter = $source.counter | Where-Object type -EQ $type
                $covered = [int]$counter.covered
                $missed = [int]$counter.missed
                $total = $covered + $missed
                $row[$type] = @{ covered = $covered; missed = $missed; percent = $(if ($total) { [Math]::Round(100 * $covered / $total, 2) } else { $null }) }
                $totals[$type].covered += $covered
                $totals[$type].missed += $missed
            }
            [pscustomobject]$row
        }
    }
)
$missing = @($selected.Keys | Where-Object { -not $selected[$_] })
if ($missing.Count) { throw "Selected sources missing from coverage XML: $($missing -join ', ')" }

$failed = @()
foreach ($type in $types) {
    $count = $totals[$type]
    $total = $count.covered + $count.missed
    if ($total -eq 0) { throw "No $type counters in selected sources." }
    $percent = 100 * $count.covered / $total
    $totals[$type].percent = [Math]::Round($percent, 2)
    Write-Host ("{0}: {1}/{2} ({3:N2}%)" -f $type, $count.covered, $total, $percent)
    if ($percent -lt $configuration.minimumPercent) { $failed += $type }
}
$outputDirectory = Join-Path $repo "app/build/reports/coverage/business"
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
[ordered]@{
    description = $configuration.description
    minimumPercent = $configuration.minimumPercent
    sourceCount = $rows.Count
    totals = $totals
    sources = @($rows | Sort-Object source)
} | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $outputDirectory "summary.json") -Encoding utf8
$summary = @(
    "# Business Coverage"
    ""
    $configuration.description
    ""
    "Source files: $($rows.Count). Minimum for each aggregate metric: $($configuration.minimumPercent)%."
    ""
    "| Metric | Covered | Total | Coverage |"
    "| --- | ---: | ---: | ---: |"
    foreach ($type in $types) {
        $count = $totals[$type]
        "| $type | $($count.covered) | $($count.covered + $count.missed) | $($count.percent)% |"
    }
    ""
    "This is an aggregate for the scope in coverage/business-scope.json, not full-app or per-file coverage."
    "See summary.json for every included source and its counters; the original JaCoCo report is preserved separately."
)
$summary | Set-Content (Join-Path $outputDirectory "summary.md") -Encoding utf8
Write-Host "Business sources: $($rows.Count). Report: $outputDirectory/summary.json"
if ($failed.Count -gt 0 -and -not $ReportOnly) {
    throw "Business coverage below $($configuration.minimumPercent)%: $($failed -join ', ')"
}
