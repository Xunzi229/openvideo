package com.example.openvideo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCategoryTest {

    @Test
    fun categoryOrderMatchesHomeTabs() {
        assertEquals(
            listOf(HomeCategory.ALL, HomeCategory.RECENT, HomeCategory.FAVORITES),
            HomeCategory.entries
        )
    }
}
