package com.openvideo.app.core.player

import android.app.Application
import android.graphics.Bitmap
import android.os.Environment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaScannerConnection
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LegacyScreenshotSaveTest {
    @Test fun savedScreenshotIsReadableAndSubmittedToTheSystemGalleryScanner() {
        val app = RuntimeEnvironment.getApplication()
        val controller = PlayerMediaExportController(app)
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val save = controller.javaClass.getDeclaredMethod("writeScreenshot", Bitmap::class.java).apply {
            isAccessible = true
        }
        val path = save.invoke(controller, bitmap) as String
        try {
            val file = File(path)
            assertEquals(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OpenVideo"), file.parentFile)
            assertTrue(file.exists())
            assertTrue(file.length() > 0)
            assertTrue(ShadowMediaScannerConnection.getSavedPaths().contains(path))
            assertTrue(ShadowMediaScannerConnection.getSavedMimeTypes().contains("image/jpeg"))
        } finally { File(path).delete(); bitmap.recycle() }
    }
}
