#Requires -Version 5.1
<#
.SYNOPSIS
  Phase 0 锁屏 / MediaSession / 后台播放 ADB 回归脚本（可重复执行）。

.DESCRIPTION
  1. 可选：跑单元测试 + assembleDebug
  2. adb install 安装 Debug APK
  3. 授权 + 开启 bg_audio
  4. 自动启动 App、进入播放、切后台
  5. 校验 PlaybackService / OpenVideoSession / play-pause

.PARAMETER SkipBuild
  跳过 Gradle 构建，仅做 ADB 回归（需已有 APK）。

.PARAMETER SkipUnitTests
  构建但不跑 testDebugUnitTest。

.EXAMPLE
  .\tools\adb\phase0-media-session-regression.ps1

.EXAMPLE
  .\tools\adb\phase0-media-session-regression.ps1 -SkipBuild
#>
[CmdletBinding()]
param(
    [switch] $SkipBuild,
    [switch] $SkipUnitTests
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$Package = 'com.example.openvideo'
$ApkPath = Join-Path $PSScriptRoot '..\..\app\build\outputs\apk\debug\app-debug.apk'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

function Write-Step([string] $Message) {
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Assert-AdbDevice {
    $lines = @(& adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice\s*$' })
    if (-not $lines -or $lines.Count -eq 0) {
        throw 'No adb device in "device" state. Connect a phone and enable USB debugging.'
    }
    $deviceId = ($lines[0].Trim() -split '\s+')[0]
    Write-Step "ADB device: $deviceId"
}

function Invoke-Gradle([string[]] $Tasks) {
    Push-Location $RepoRoot
    try {
        & .\gradlew.bat @Tasks
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $($Tasks -join ' ')" }
    } finally {
        Pop-Location
    }
}

function Set-BackgroundAudioPref {
    $existing = adb shell run-as $Package cat shared_prefs/player_settings.xml 2>$null
    if ($existing -match 'name="bg_audio"') {
        $updated = $existing -replace 'name="bg_audio" value="false"', 'name="bg_audio" value="true"'
        if ($updated -eq $existing) {
            $updated = $existing -replace '(<map>)', "`$1`n    <boolean name=`"bg_audio`" value=`"true`" />"
        }
    } else {
        $updated = @'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="bg_audio" value="true" />
</map>
'@
    }
    $updated | adb shell run-as $Package tee shared_prefs/player_settings.xml | Out-Null
}

function Wait-ForFocus([string] $Pattern, [int] $TimeoutSec = 30) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $focus = (& adb shell dumpsys window | Select-String 'mCurrentFocus').Line
        if ($focus -match $Pattern) { return $focus }
        Start-Sleep -Seconds 1
    }
    throw "Timed out waiting for focus matching: $Pattern (last: $focus)"
}

function Get-MediaSessionDump {
    return (& adb shell dumpsys media_session) -join "`n"
}

function Test-PlaybackServiceRunning {
    $services = (& adb shell dumpsys activity services $Package) -join "`n"
    return ($services -match 'PlaybackService') -and ($services -match 'isForeground=true')
}

function Get-PlaybackStateLine {
    param([string] $Dump)
    return ($Dump -split "`n" | Where-Object { $_ -match 'state=PlaybackState' } | Select-Object -First 1)
}

function Unlock-Device {
    adb shell input keyevent KEYCODE_WAKEUP | Out-Null
    adb shell wm dismiss-keyguard | Out-Null
    Start-Sleep -Milliseconds 800
    adb shell input swipe 540 1800 540 800 300 | Out-Null
    Start-Sleep -Milliseconds 800
}

function Start-PlaybackViaUi {
    Write-Step 'Launch app and open first video'
    adb shell am force-stop $Package | Out-Null
    Start-Sleep -Seconds 1
    adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
    Start-Sleep -Seconds 8

    adb shell input tap 300 420 | Out-Null
    Start-Sleep -Seconds 2
    adb shell input tap 540 850 | Out-Null
    Wait-ForFocus 'PlayerActivity' 20 | Out-Null
    Start-Sleep -Seconds 15
}

# -- Main --
Write-Step "Repo: $RepoRoot"
Assert-AdbDevice

if (-not $SkipBuild) {
    if (-not $SkipUnitTests) {
        Write-Step 'Run :app:testDebugUnitTest'
        Invoke-Gradle @(':app:testDebugUnitTest')
    }
    Write-Step 'Run :app:assembleDebug'
    Invoke-Gradle @(':app:assembleDebug')
}

if (-not (Test-Path $ApkPath)) {
    throw "APK not found: $ApkPath (run without -SkipBuild first)"
}

Write-Step 'Install debug APK'
adb install -r $ApkPath | Out-Null

Write-Step 'Grant runtime permissions'
adb shell pm grant $Package android.permission.READ_MEDIA_VIDEO 2>$null
adb shell pm grant $Package android.permission.POST_NOTIFICATIONS 2>$null

Write-Step 'Enable bg_audio for background PlaybackService'
Set-BackgroundAudioPref

Unlock-Device
Set-BackgroundAudioPref
Start-PlaybackViaUi
Set-BackgroundAudioPref

Write-Step 'Send HOME to background playback'
adb shell input keyevent KEYCODE_HOME | Out-Null
Start-Sleep -Seconds 5

if (-not (Test-PlaybackServiceRunning)) {
    throw 'PlaybackService is not running in foreground after HOME'
}
Write-Host 'OK  PlaybackService foreground' -ForegroundColor Green

$dump = Get-MediaSessionDump
if ($dump -notmatch 'OpenVideoSession') {
    throw 'OpenVideoSession not found in media_session dump'
}
Write-Host 'OK  OpenVideoSession registered' -ForegroundColor Green

$playingLine = Get-PlaybackStateLine -Dump $dump
if ($playingLine -notmatch 'state=3\b') {
    throw "Expected MediaSession state=3 (playing), got: $playingLine"
}
Write-Host "OK  MediaSession playing: $($playingLine.Trim())" -ForegroundColor Green

Write-Step 'Toggle play/pause via media_session'
adb shell cmd media_session dispatch play-pause | Out-Null
Start-Sleep -Seconds 2
$pausedLine = Get-PlaybackStateLine -Dump (Get-MediaSessionDump)
if ($pausedLine -notmatch 'state=2\b') {
    throw "Expected paused state=2 after toggle, got: $pausedLine"
}
Write-Host "OK  Paused: $($pausedLine.Trim())" -ForegroundColor Green

adb shell cmd media_session dispatch play-pause | Out-Null
Start-Sleep -Seconds 2
$resumedLine = Get-PlaybackStateLine -Dump (Get-MediaSessionDump)
if ($resumedLine -notmatch 'state=3\b') {
    throw "Expected resumed state=3, got: $resumedLine"
}
Write-Host "OK  Resumed: $($resumedLine.Trim())" -ForegroundColor Green

Write-Host ''
Write-Host 'Phase 0 media session regression: PASS' -ForegroundColor Green
