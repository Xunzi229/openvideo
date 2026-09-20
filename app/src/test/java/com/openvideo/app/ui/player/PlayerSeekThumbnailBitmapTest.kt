package com.openvideo.app.ui.player

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlayerSeekThumbnailBitmapTest {
    @Test
    fun scalingReleasesOriginalFrameAndKeepsPreviewWithinBounds() {
        val loader = PlayerSeekThumbnailLoader(RuntimeEnvironment.getApplication())
        val original = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val preview = loader.scaleForPreview(original)
        assertTrue(original.isRecycled)
        assertFalse(preview.isRecycled)
        assertEquals(240, preview.width)
        assertEquals(135, preview.height)
        preview.recycle()
        loader.release()
    }

    @Test
    fun smallFrameIsReusedWithoutRecycling() {
        val loader = PlayerSeekThumbnailLoader(RuntimeEnvironment.getApplication())
        val original = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        assertSame(original, loader.scaleForPreview(original))
        assertFalse(original.isRecycled)
        original.recycle()
        loader.release()
    }
}
