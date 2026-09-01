package com.example.openvideo.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ReleasePageLauncherSourceTest {

    @Test
    fun updateNavigationUsesSystemBrowserAndReleasePageOnly() {
        val launcher = rootText(
            "app", "src", "main", "java", "com", "example", "openvideo",
            "core", "update", "ReleasePageLauncher.kt"
        )
        val settings = rootText(
            "app", "src", "main", "java", "com", "example", "openvideo",
            "ui", "settings", "SettingsViewModel.kt"
        )

        assertTrue(launcher.contains("UpdateUrlPolicy.isTrustedReleasePage(releasePageUrl)"))
        assertTrue(launcher.contains("Intent.makeMainSelectorActivity"))
        assertTrue(launcher.contains("Intent.CATEGORY_APP_BROWSER"))
        assertTrue(launcher.contains("data = releasePageUrl.toUri()"))
        assertTrue(settings.contains("ReleasePageLauncher.open(activityContext, release.releaseHtmlUrl)"))
        assertFalse(settings.contains("browserDownloadUrl"))
        assertFalse(settings.contains("UpdateApkInstaller"))
    }

    @Test
    fun manifestDoesNotRequestAppInstallationOrExposeUpdateFiles() {
        val manifest = rootText("app", "src", "main", "AndroidManifest.xml")

        assertFalse(manifest.contains("REQUEST_INSTALL_PACKAGES"))
        assertFalse(manifest.contains("androidx.core.content.FileProvider"))
        assertFalse(manifest.contains("file_paths_update"))
    }

    private fun rootText(vararg parts: String): String =
        String(Files.readAllBytes(rootFile(*parts)))

    private fun rootFile(vararg parts: String): Path =
        parts.fold(Paths.get("")) { path, part -> path.resolve(part) }
            .let { relative ->
                sequenceOf(relative, Paths.get("..").resolve(relative)).firstOrNull(Files::exists)
                    ?: relative
            }
}
