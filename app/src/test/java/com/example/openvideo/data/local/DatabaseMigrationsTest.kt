package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationsTest {

    @Test
    fun registersMigrationFromVersionOneToTwo() {
        assertTrue(DatabaseMigrations.ALL.any { it.startVersion == 1 && it.endVersion == 2 })
    }

    @Test
    fun registersMigrationFromVersionFiveToSix() {
        assertTrue(DatabaseMigrations.ALL.any { it.startVersion == 5 && it.endVersion == 6 })
    }

    @Test
    fun registersMigrationFromVersionSixToSeven() {
        assertTrue(DatabaseMigrations.ALL.any { it.startVersion == 6 && it.endVersion == 7 })
    }

    @Test
    fun registersMigrationFromVersionSevenToEight() {
        assertTrue(DatabaseMigrations.ALL.any { it.startVersion == 7 && it.endVersion == 8 })
    }

    @Test
    fun registersMigrationFromVersionEightToNine() {
        assertTrue(DatabaseMigrations.ALL.any { it.startVersion == 8 && it.endVersion == 9 })
    }

    @Test
    fun registersMigrationFromVersionNineToTen() {
        assertTrue(DatabaseMigrations.ALL.any { it.startVersion == 9 && it.endVersion == 10 })
    }
}
