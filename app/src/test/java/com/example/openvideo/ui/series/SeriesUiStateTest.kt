package com.example.openvideo.ui.series

import com.example.openvideo.data.local.SeriesEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class SeriesUiStateTest {

    @Test
    fun mapsSeriesEntityToListUiStateWithoutChangingIdentityFields() {
        val state = uiStateFrom(
            SeriesEntity(
                seriesId = 9L,
                title = "Show Name",
                normalizedTitleKey = "show name",
                folderPath = "/storage/emulated/0/movies/show",
                posterPath = "/storage/emulated/0/movies/show/poster.jpg",
                createdAt = 100L,
                updatedAt = 200L
            )
        )

        assertEquals(9L, longProperty(state, "seriesId"))
        assertEquals("Show Name", stringProperty(state, "title"))
        assertEquals("/storage/emulated/0/movies/show", stringProperty(state, "folderPath"))
        assertEquals("/storage/emulated/0/movies/show/poster.jpg", nullableStringProperty(state, "posterPath"))
        assertEquals(200L, longProperty(state, "updatedAt"))
    }

    @Test
    fun blankTitleFallsBackToNormalizedTitleKeyForDisplay() {
        val state = uiStateFrom(
            SeriesEntity(
                seriesId = 1L,
                title = "",
                normalizedTitleKey = "fallback show",
                folderPath = "/shows/fallback",
                posterPath = null,
                createdAt = 10L,
                updatedAt = 20L
            )
        )

        assertEquals("fallback show", stringProperty(state, "title"))
        assertEquals(null, nullableStringProperty(state, "posterPath"))
    }

    private fun uiStateFrom(entity: SeriesEntity): Any {
        val stateClass = try {
            Class.forName("com.example.openvideo.ui.series.SeriesUiState")
        } catch (e: ClassNotFoundException) {
            fail("SeriesUiState must exist before a series list can render")
            throw e
        }
        val companion = stateClass.getDeclaredField("Companion").get(null)
        val method = companion.javaClass.getDeclaredMethod("from", SeriesEntity::class.java)
        return method.invoke(companion, entity) ?: fail("SeriesUiState.from returned null")
    }

    private fun stringProperty(instance: Any, property: String): String =
        instance.javaClass.getMethod(getter(property)).invoke(instance) as String

    private fun nullableStringProperty(instance: Any, property: String): String? =
        instance.javaClass.getMethod(getter(property)).invoke(instance) as String?

    private fun longProperty(instance: Any, property: String): Long =
        instance.javaClass.getMethod(getter(property)).invoke(instance) as Long

    private fun getter(property: String): String =
        "get" + property.substring(0, 1).uppercase() + property.substring(1)
}
