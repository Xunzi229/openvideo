# Wave 3.4 adb smoke: prefs + logcat OVContentFrame (PlayerActivity is not exported)
$ErrorActionPreference = "Stop"
$pkg = "com.example.openvideo"
$outDir = Join-Path $PSScriptRoot "..\output\adb-verify"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Push-PlayerPrefs([string]$xmlPath) {
    adb push $xmlPath /data/local/tmp/ov_player_settings.xml | Out-Null
    adb shell am force-stop $pkg
    adb shell run-as $pkg cp /data/local/tmp/ov_player_settings.xml shared_prefs/player_settings.xml
}

function Launch-And-Play-Portrait {
    adb shell pm grant $pkg android.permission.READ_MEDIA_VIDEO 2>$null
    adb shell am start -n "$pkg/.ui.MainActivity" | Out-Null
    Start-Sleep -Seconds 6
    # 视频 Tab
    adb shell input tap 405 2216
    Start-Sleep -Seconds 2
    # Camera 文件夹 chip
    adb shell input tap 946 625
    Start-Sleep -Seconds 2
    # 列表首项
    adb shell input tap 540 1009
    Start-Sleep -Seconds 6
}

Push-PlayerPrefs (Join-Path $PSScriptRoot "..\output\adb_player_settings_zoom.xml")
adb logcat -c
Launch-And-Play-Portrait
adb logcat -d -s OVContentFrame:V | Tee-Object (Join-Path $outDir "content_frame.log")
adb shell screencap -p /sdcard/ov_verify.png
adb pull /sdcard/ov_verify.png (Join-Path $outDir "portrait_center_16_9.png") | Out-Null
Write-Host "Done. See $outDir"
