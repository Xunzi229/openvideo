package com.example.openvideo.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.example.openvideo.R
import com.example.openvideo.core.network.WebDavMemoryCache
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.core.prefs.AspectRatio
import com.example.openvideo.core.prefs.PlayerPrefs
import com.example.openvideo.core.prefs.SettingsBackupExporter
import com.example.openvideo.core.prefs.SettingsBackupFileWriter
import com.example.openvideo.core.prefs.SettingsBackupImporter
import com.example.openvideo.core.prefs.SettingsBackupSchema
import com.example.openvideo.core.prefs.ThemeMode
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionStyle
import com.example.openvideo.core.ui.AppleAlertDialog
import com.example.openvideo.core.ui.AppleHud
import com.example.openvideo.core.update.GitHubReleaseChecker
import com.example.openvideo.core.update.UpdateApkInstaller
import com.example.openvideo.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val appPrefs: AppPrefs,
    private val playerPrefs: PlayerPrefs,
    private val repository: VideoRepository,
    private val webDavMemoryCache: WebDavMemoryCache
) : AndroidViewModel(application) {

    val themeMode: ThemeMode get() = appPrefs.themeMode
    val language: String get() = appPrefs.language
    val defaultSpeed: Float get() = DefaultPlayerSettings.supportedSpeedOrDefault(playerPrefs.speed)
    val defaultRatio: AspectRatio get() = DefaultPlayerSettings.aspectRatioOrDefault(playerPrefs.aspectRatio)
    private val _cacheSize = MutableStateFlow("0 MB")
    val cacheSize: StateFlow<String> = _cacheSize

    private val _historyCount = MutableStateFlow(0)
    val historyCount: StateFlow<Int> = _historyCount

    private val _updateBadgeVisible = MutableStateFlow(appPrefs.githubUpdateBadgeVisible)
    val updateBadgeVisible: StateFlow<Boolean> = _updateBadgeVisible
    private val updateCheckRunning = AtomicBoolean(false)

    init {
        computeCacheSize()
        observeHistoryCount()
    }

    /**
     * Starts silently on app launch (from MainActivity). No toast/dialog.
     * Throttled to once per 24h unless [force].
     */
    fun checkForAppUpdateSilently(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!force && !GitHubReleaseChecker.shouldRunPeriodicCheck(appPrefs.lastGitHubReleaseCheckMs)) {
                return@launch
            }
            val release = fetchLatestReleaseOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                applyGitHubReleaseCheckResult(release)
            }
        }
    }

    /**
     * Checks GitHub latest release. Shows HUD immediately, then an alert if a newer APK exists.
     */
    fun onCheckUpdateClick(activityContext: Context) {
        if (!updateCheckRunning.compareAndSet(false, true)) return
        AppleHud.show(activityContext, R.string.settings_update_checking)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val release = fetchLatestReleaseOrNull()
                if (release == null) {
                    withContext(Dispatchers.Main) {
                        AppleHud.show(activityContext, R.string.settings_update_check_failed)
                    }
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    applyGitHubReleaseCheckResult(release)
                }
                if (!GitHubReleaseChecker.isRemoteNewer(release.tagName, installedVersionName())) {
                    withContext(Dispatchers.Main) {
                        AppleHud.show(activityContext, R.string.settings_already_latest)
                    }
                    return@launch
                }
                val apk = GitHubReleaseChecker.selectApkForAbi(release.assets, Build.SUPPORTED_ABIS)
                withContext(Dispatchers.Main) {
                    AppleHud.dismiss()
                    promptAvailableUpdate(activityContext, release, apk)
                }
            } finally {
                updateCheckRunning.set(false)
            }
        }
    }

    private fun promptAvailableUpdate(
        activityContext: Context,
        release: GitHubReleaseChecker.LatestRelease,
        apk: GitHubReleaseChecker.ReleaseAsset?
    ) {
        val app = getApplication<Application>()
        AppleAlertDialog.show(
            context = activityContext,
            title = app.getString(R.string.settings_update_available_title),
            message = app.getString(R.string.settings_update_available_message, release.tagName),
            actions = listOf(
                AppleAction(app.getString(R.string.action_cancel), AppleActionStyle.CANCEL),
                AppleAction(app.getString(R.string.settings_update_now)) {
                    downloadAndInstallUpdate(activityContext, release, apk)
                }
            )
        )
    }

    private fun downloadAndInstallUpdate(
        activityContext: Context,
        release: GitHubReleaseChecker.LatestRelease,
        apk: GitHubReleaseChecker.ReleaseAsset?
    ) {
        val fallbackUrl = apk?.browserDownloadUrl ?: release.releaseHtmlUrl
        if (apk == null) {
            AppleHud.show(activityContext, R.string.settings_update_no_apk_asset)
            openInBrowser(activityContext, fallbackUrl)
            return
        }
        val app = getApplication<Application>()
        AppleHud.show(activityContext, R.string.settings_update_downloading, long = true)
        viewModelScope.launch(Dispatchers.IO) {
            val ua = "OpenVideo/${installedVersionName()} (Android)"
            val expectedHex = GitHubReleaseChecker.resolveExpectedSha256Hex(release.assets, apk) { url ->
                GitHubReleaseChecker.fetchUrlText(url, ua)
            }
            if (expectedHex == null) {
                withContext(Dispatchers.Main) {
                    AppleHud.show(activityContext, R.string.settings_update_no_checksum_browser, long = true)
                    openInBrowser(activityContext, apk.browserDownloadUrl)
                }
                return@launch
            }
            val dest = UpdateApkInstaller.cacheApkFile(app)
            if (!UpdateApkInstaller.downloadApk(apk.browserDownloadUrl, dest, ua)) {
                withContext(Dispatchers.Main) {
                    AppleHud.show(activityContext, R.string.settings_update_download_failed)
                    openInBrowser(activityContext, apk.browserDownloadUrl)
                }
                return@launch
            }
            if (!UpdateApkInstaller.shaMatches(dest, expectedHex)) {
                dest.delete()
                withContext(Dispatchers.Main) {
                    AppleHud.show(activityContext, R.string.settings_update_sha_mismatch, long = true)
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !activityContext.packageManager.canRequestPackageInstalls()
                ) {
                    AppleHud.show(activityContext, R.string.settings_update_allow_install_or_browser, long = true)
                    openInBrowser(activityContext, apk.browserDownloadUrl)
                    return@withContext
                }
                try {
                    AppleHud.dismiss()
                    activityContext.startActivity(UpdateApkInstaller.buildInstallIntent(activityContext, dest))
                } catch (_: Exception) {
                    AppleHud.show(activityContext, R.string.settings_update_install_failed_browser)
                    openInBrowser(activityContext, apk.browserDownloadUrl)
                }
            }
        }
    }

    private fun openInBrowser(context: Context, url: String) {
        if (url.isBlank()) return
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun fetchLatestReleaseOrNull(): GitHubReleaseChecker.LatestRelease? {
        val ua = "OpenVideo/${installedVersionName()} (Android)"
        return GitHubReleaseChecker.fetchLatestRelease(ua)
    }

    private fun applyGitHubReleaseCheckResult(release: GitHubReleaseChecker.LatestRelease) {
        val newer = GitHubReleaseChecker.isRemoteNewer(release.tagName, installedVersionName())
        val url = if (newer) GitHubReleaseChecker.preferredDownloadUrl(release, Build.SUPPORTED_ABIS) else ""
        appPrefs.lastGitHubReleaseCheckMs = System.currentTimeMillis()
        appPrefs.githubUpdateBadgeVisible = newer
        appPrefs.githubPendingDownloadUrl = url
        _updateBadgeVisible.value = newer
    }

    fun setThemeMode(mode: ThemeMode) {
        appPrefs.themeMode = mode
        AppSettingsApplier.apply(appPrefs)
    }

    fun setLanguage(lang: String) {
        appPrefs.language = lang
        AppSettingsApplier.apply(appPrefs)
    }

    fun setDefaultSpeed(speed: Float) {
        playerPrefs.speed = DefaultPlayerSettings.supportedSpeedOrDefault(speed)
    }

    fun setDefaultRatio(ratio: AspectRatio) {
        playerPrefs.aspectRatio = DefaultPlayerSettings.aspectRatioOrDefault(ratio)
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            ctx.cacheDir?.deleteRecursively()
            webDavMemoryCache.clear()
            withContext(Dispatchers.Main) {
                Glide.get(ctx).clearMemory()
            }
            computeCacheSize()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun buildSettingsExportJson(): String =
        SettingsBackupExporter.exportJson(playerPrefs, appPrefs)

    suspend fun writeSettingsExportTo(context: Context, uri: Uri): SettingsBackupFileWriter.Result =
        withContext(Dispatchers.IO) {
            val json = buildSettingsExportJson()
            SettingsBackupFileWriter.writeJson(context.contentResolver, uri, json)
        }

    /** 设置导入结果。 */
    sealed class ImportResult {
        object Success : ImportResult()
        data class ParseFailure(val reason: SettingsBackupSchema.Reason) : ImportResult()
        object ReadFailure : ImportResult()
    }

    /**
     * 从 [uri] 读取 JSON，解析并导入到 PlayerPrefs / AppPrefs。
     * 在 IO 线程执行，调用方可在 Main 上收取 [ImportResult]。
     */
    suspend fun readAndImportSettings(context: Context, uri: Uri): ImportResult =
        withContext(Dispatchers.IO) {
            val json = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: return@withContext ImportResult.ReadFailure
            } catch (_: Exception) {
                return@withContext ImportResult.ReadFailure
            }
            when (val result = SettingsBackupSchema.decode(json)) {
                is SettingsBackupSchema.ParseResult.Success -> {
                    SettingsBackupImporter.apply(result.document, playerPrefs, appPrefs)
                    ImportResult.Success
                }
                is SettingsBackupSchema.ParseResult.Failure ->
                    ImportResult.ParseFailure(result.reason)
            }
        }

    private fun computeCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val size = ctx.cacheDir?.walkTopDown()?.sumOf { it.length() } ?: 0L
            _cacheSize.value = formatSize(size)
        }
    }

    private fun observeHistoryCount() {
        viewModelScope.launch {
            repository.getHistory().collect { list ->
                _historyCount.value = list.size
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format("%.1f MB", mb)
    }

    fun installedVersionName(): String {
        val ctx = getApplication<Application>()
        return try {
            val info = if (Build.VERSION.SDK_INT >= 33) {
                ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            }
            info.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
}
