# Unified commit entry (avoid raw "git commit" in agent shells; use this script.)
param(
    [Parameter(Mandatory = $true)]
    [string] $Message
)
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

git add -A

$staged = git diff --cached --name-only
if (-not $staged) {
    Write-Host "Nothing to commit (no staged changes)."
    exit 0
}

git commit -m $Message
Write-Host "Committed: $Message"
