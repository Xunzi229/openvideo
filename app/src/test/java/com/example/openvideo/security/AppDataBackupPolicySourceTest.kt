package com.example.openvideo.security

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class AppDataBackupPolicySourceTest {

    @Test
    fun manifestAndRulesExcludePrivateAppDataFromCloudAndDeviceTransfer() {
        val manifest = read("src/main/AndroidManifest.xml")
        val legacyRules = read("src/main/res/xml/backup_rules.xml")
        val modernRules = read("src/main/res/xml/data_extraction_rules.xml")

        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""))
        assertTrue(manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""))
        listOf("database", "sharedpref", "file", "root").forEach { domain ->
            assertTrue(legacyRules.contains("domain=\"$domain\" path=\".\""))
            assertTrue(modernRules.contains("domain=\"$domain\" path=\".\""))
        }
        assertTrue(modernRules.contains("<cloud-backup>"))
        assertTrue(modernRules.contains("<device-transfer>"))
    }

    private fun read(relative: String): String {
        val direct = Paths.get(relative)
        val path = if (Files.exists(direct)) direct else Paths.get("app").resolve(relative)
        return String(Files.readAllBytes(path))
    }
}
