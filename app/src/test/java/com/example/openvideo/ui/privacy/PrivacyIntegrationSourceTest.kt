package com.example.openvideo.ui.privacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class PrivacyIntegrationSourceTest {

    @Test
    fun fragmentUsesInjectedSingletonAndManagerMigratesLegacyPassword() {
        val fragment = read("ui/privacy/PrivacyFragment.kt")
        val manager = read("ui/privacy/PrivacyManager.kt")

        assertTrue(fragment.contains("@Inject lateinit var privacyManager: PrivacyManager"))
        assertFalse(fragment.contains("PrivacyManager(requireContext())"))
        assertTrue(manager.contains("PrivacyPasswordPolicy.verify"))
        assertTrue(manager.contains("result.valid && result.needsUpgrade"))
        assertTrue(manager.contains("PrivacyPathPolicy::canonical"))
    }

    private fun read(relative: String): String {
        val source = "src/main/java/com/example/openvideo/$relative"
        val direct = Paths.get(source)
        val path = if (Files.exists(direct)) direct else Paths.get("app").resolve(source)
        return String(Files.readAllBytes(path))
    }
}
