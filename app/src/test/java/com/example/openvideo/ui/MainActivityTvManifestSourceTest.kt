package com.example.openvideo.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MainActivityTvManifestSourceTest {

    @Test
    fun manifestDeclaresOptionalTvLauncherSupportWithoutDroppingPhoneLauncher() {
        val manifest = String(Files.readAllBytes(androidManifest()))
        val bannerPath = tvBannerDrawable()

        assertTrue(manifest.contains("""<uses-feature android:name="android.software.leanback" android:required="false" />"""))
        assertTrue(manifest.contains("""<uses-feature android:name="android.hardware.touchscreen" android:required="false" />"""))
        assertTrue(manifest.contains("""android:banner="@drawable/bg_tv_banner""""))
        assertTrue(manifest.contains("""<category android:name="android.intent.category.LAUNCHER" />"""))
        assertTrue(manifest.contains("""<category android:name="android.intent.category.LEANBACK_LAUNCHER" />"""))
        assertTrue("TV banner drawable should exist", Files.exists(bannerPath))

        val banner = String(Files.readAllBytes(bannerPath))
        assertTrue(banner.contains("""android:width="320dp""""))
        assertTrue(banner.contains("""android:height="180dp""""))
        assertTrue(banner.contains("""@drawable/ic_launcher_foreground"""))
    }

    private fun androidManifest(): Path {
        val relativePath = Paths.get("src", "main", "AndroidManifest.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }

    private fun tvBannerDrawable(): Path {
        val relativePath = Paths.get("src", "main", "res", "drawable", "bg_tv_banner.xml")
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).firstOrNull(Files::exists)
            ?: Paths.get("app").resolve(relativePath)
    }
}
