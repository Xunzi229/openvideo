# One command: sign release unsigned apk with local keystore.
param(
    [Parameter(Mandatory = $true)]
    [string] $KeystorePath,

    [Parameter(Mandatory = $true)]
    [string] $KeyAlias,

    [string] $UnsignedApkPath = "",
    [string] $OutputApkPath = "",
    [string] $StorePassword = "",
    [string] $KeyPassword = "",
    [switch] $NoOpenDir
)

$ErrorActionPreference = "Stop"

function Resolve-ApkSignerPath {
    $cmd = Get-Command "apksigner.bat" -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $cmd = Get-Command "apksigner" -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $sdkCandidates = @()
    if ($env:ANDROID_SDK_ROOT) { $sdkCandidates += $env:ANDROID_SDK_ROOT }
    if ($env:ANDROID_HOME) { $sdkCandidates += $env:ANDROID_HOME }
    if ($env:LOCALAPPDATA) { $sdkCandidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk") }

    foreach ($sdk in $sdkCandidates | Select-Object -Unique) {
        if (-not (Test-Path $sdk)) { continue }
        $buildToolsDir = Join-Path $sdk "build-tools"
        if (-not (Test-Path $buildToolsDir)) { continue }

        $versions = Get-ChildItem $buildToolsDir -Directory | Sort-Object Name -Descending
        foreach ($v in $versions) {
            $candidate = Join-Path $v.FullName "apksigner.bat"
            if (Test-Path $candidate) { return $candidate }
            $candidate = Join-Path $v.FullName "apksigner"
            if (Test-Path $candidate) { return $candidate }
        }
    }

    throw "Cannot find apksigner. Please install Android SDK Build-Tools or add apksigner to PATH."
}

function Resolve-UnsignedApkPath([string] $repoRoot, [string] $manualPath) {
    if (-not [string]::IsNullOrWhiteSpace($manualPath)) {
        $resolved = Resolve-Path $manualPath -ErrorAction Stop
        return $resolved.Path
    }

    $matches = Get-ChildItem (Join-Path $repoRoot "app\build\outputs") -Recurse -File -Filter "*release*unsigned*.apk" |
        Sort-Object LastWriteTime -Descending
    if ($matches -and $matches.Count -gt 0) {
        return $matches[0].FullName
    }

    throw "Cannot auto-find unsigned release apk. Please pass -UnsignedApkPath explicitly."
}

function Resolve-AppVersionName([string] $repoRoot) {
    $gradleFile = Join-Path $repoRoot "app\build.gradle.kts"
    if (-not (Test-Path $gradleFile)) {
        throw "Cannot find app build file: $gradleFile"
    }

    $source = Get-Content -Raw -Path $gradleFile
    $match = [regex]::Match($source, 'versionName\s*=\s*"([^"]+)"')
    if (-not $match.Success) {
        throw "Cannot read versionName from: $gradleFile"
    }

    return $match.Groups[1].Value
}

function Find-ExistingKeystores([string] $repoRoot) {
    $roots = @()
    if ($env:USERPROFILE) { $roots += $env:USERPROFILE }
    $roots += (Join-Path $repoRoot "keys")
    $roots += $repoRoot

    $all = @()
    foreach ($root in ($roots | Select-Object -Unique)) {
        if (-not (Test-Path $root)) { continue }
        $matches = Get-ChildItem $root -Recurse -File -Include *.jks,*.keystore -ErrorAction SilentlyContinue
        if ($matches) { $all += $matches }
    }

    return $all |
        Sort-Object LastWriteTime -Descending |
        Select-Object -ExpandProperty FullName -Unique
}

function Resolve-KeytoolPath {
    $cmd = Get-Command "keytool.exe" -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $cmd = Get-Command "keytool" -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $fallbacks = @(
        "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
        "C:\Program Files\Android\Android Studio\jre\bin\keytool.exe"
    )
    foreach ($path in $fallbacks) {
        if (Test-Path $path) { return $path }
    }
    throw "Cannot find keytool. Please install JDK 17 and ensure keytool is in PATH."
}

function Ensure-KeystorePath([string] $repoRoot, [string] $inputPath, [string] $defaultAlias) {
    if (Test-Path $inputPath) {
        return (Resolve-Path $inputPath -ErrorAction Stop).Path
    }

    Write-Host ""
    Write-Host "未找到 keystore: $inputPath"
    $candidates = Find-ExistingKeystores $repoRoot
    if ($candidates.Count -gt 0) {
        Write-Host "检测到以下可用 keystore："
        for ($i = 0; $i -lt $candidates.Count; $i++) {
            Write-Host ("[{0}] {1}" -f ($i + 1), $candidates[$i])
        }
    } else {
        Write-Host "未检测到现有 keystore。"
    }

    Write-Host ""
    Write-Host "操作选项：输入序号=使用已有；输入 N=在目标路径创建新 keystore；直接回车=取消"
    $choice = (Read-Host "请选择").Trim()

    if ([string]::IsNullOrWhiteSpace($choice)) {
        throw "Canceled by user."
    }

    $num = 0
    if ([int]::TryParse($choice, [ref]$num)) {
        if ($num -ge 1 -and $num -le $candidates.Count) {
            return (Resolve-Path $candidates[$num - 1] -ErrorAction Stop).Path
        }
        throw "Invalid index: $choice"
    }

    if ($choice -match '^(n|N)$') {
        $targetDir = Split-Path -Parent $inputPath
        if (-not [string]::IsNullOrWhiteSpace($targetDir) -and -not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }

        $keytool = Resolve-KeytoolPath
        Write-Host "开始创建新 keystore（将进入 keytool 交互）..."
        & $keytool -genkeypair -v `
            -keystore $inputPath `
            -alias $defaultAlias `
            -keyalg RSA `
            -keysize 2048 `
            -validity 10000

        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $inputPath)) {
            throw "Failed to create keystore at: $inputPath"
        }

        return (Resolve-Path $inputPath -ErrorAction Stop).Path
    }

    throw "Invalid choice: $choice"
}

function Read-Secret([string] $prompt) {
    $secure = Read-Host -AsSecureString $prompt
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$keystoreResolved = Ensure-KeystorePath $repoRoot $KeystorePath $KeyAlias
$unsignedResolved = Resolve-UnsignedApkPath $repoRoot $UnsignedApkPath

if ([string]::IsNullOrWhiteSpace($OutputApkPath)) {
    $dir = Split-Path -Parent $unsignedResolved
    $versionName = Resolve-AppVersionName $repoRoot
    $signedName = "openvideo-v{0}-app-release-signed.apk" -f $versionName
    $OutputApkPath = Join-Path $dir $signedName
}
$outputResolved = [IO.Path]::GetFullPath($OutputApkPath)

if ([string]::IsNullOrWhiteSpace($StorePassword)) {
    $StorePassword = Read-Secret "请输入 keystore 密码"
}

if ([string]::IsNullOrWhiteSpace($KeyPassword)) {
    $KeyPassword = Read-Secret "请输入 key 密码（直接回车表示与 keystore 密码一致）"
}
if ([string]::IsNullOrWhiteSpace($KeyPassword)) {
    $KeyPassword = $StorePassword
}

$apksigner = Resolve-ApkSignerPath

& $apksigner sign `
    --ks $keystoreResolved `
    --ks-key-alias $KeyAlias `
    --ks-pass ("pass:{0}" -f $StorePassword) `
    --key-pass ("pass:{0}" -f $KeyPassword) `
    --out $outputResolved `
    $unsignedResolved

if ($LASTEXITCODE -ne 0) {
    throw "apksigner sign failed with exit code $LASTEXITCODE"
}

& $apksigner verify --print-certs $outputResolved
if ($LASTEXITCODE -ne 0) {
    throw "apksigner verify failed with exit code $LASTEXITCODE"
}

Write-Host "Signed APK:"
Write-Host $outputResolved

if (-not $NoOpenDir) {
    Start-Process "explorer.exe" (Split-Path -Parent $outputResolved)
}
