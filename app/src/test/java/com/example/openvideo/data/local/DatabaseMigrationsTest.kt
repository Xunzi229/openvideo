package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationsTest {

    @Test
    fun registersMigrationFromVersionOneToTwo() {
        assertTrue(DatabaseMigrations.ALL.any { it.startVersion == 1 && it.endVersion == 2 })
    }
}
