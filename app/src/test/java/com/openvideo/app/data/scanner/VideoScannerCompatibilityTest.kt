package com.openvideo.app.data.scanner

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import com.openvideo.app.data.model.VideoItem
import com.openvideo.app.ui.home.MediaLibraryPermissionPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.util.Collections

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28, 29, 33], application = Application::class)
class VideoScannerCompatibilityTest {
    private lateinit var app: Application
    private lateinit var provider: VideoMediaProvider
    private lateinit var scanner: VideoScanner

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(*MediaLibraryPermissionPolicy.requiredPermissions())
        provider = VideoMediaProvider()
        ShadowContentResolver.registerProviderInternal("media", provider)
        scanner = VideoScanner(app)
    }

    @Test
    fun fullScanPreservesPathsDimensionsAndFilteringOnEachSupportedSchema() {
        provider.rows = listOf(VideoRow(1), VideoRow(2), VideoRow(3, size = 0))
        val videos = successfulScan()

        assertEquals(listOf(2L, 1L), videos.map { it.id })
        videos.forEach { assertVideoMetadata(it, VideoRow(it.id)) }
        val query = provider.queries.single()
        assertEquals("date_added DESC", query.sortOrder)
        assertProjectionMatchesPlatform(query)
    }

    @Test
    fun incrementalRefreshFetchesOnlyChangedAndAddedVideosAndRemovesDeletedVideos() {
        provider.rows = (1L..100L).map(::VideoRow)
        val before = successfulScan()
        provider.queries.clear()
        provider.rows = provider.rows.filterNot { it.id == 2L }.map {
            if (it.id == 1L) it.copy(duration = 9_999, orientation = 270) else it
        } + VideoRow(101)

        val after = successfulScan()
        assertEquals(100, after.size)
        assertFalse(after.any { it.id == 2L })
        assertVideoMetadata(after.first { it.id == 1L }, VideoRow(1, duration = 9_999, orientation = 270))
        assertVideoMetadata(after.first { it.id == 101L }, VideoRow(101))
        assertSame(before.first { it.id == 3L }, after.first { it.id == 3L })
        assertEquals(2, provider.queries.size)
        val fetch = provider.queries.last()
        assertEquals("_id IN (?,?)", fetch.selection)
        assertEquals(setOf("1", "101"), fetch.selectionArgs.toSet())
        provider.queries.forEach(::assertProjectionMatchesPlatform)
    }

    @Test
    fun unchangedLibraryUsesOnlyTheIndexAndReusesCachedVideos() {
        provider.rows = (1L..100L).map(::VideoRow)
        val before = successfulScan()
        provider.queries.clear()

        val after = successfulScan()
        assertEquals(before, after)
        assertSame(before.first(), after.first())
        val query = provider.queries.single()
        assertEquals(null, query.selection)
        assertEquals(null, query.sortOrder)
        assertProjectionMatchesPlatform(query)
    }

    @Test
    fun extensiveLibraryChangesStillUseTheExistingFullScanFallback() {
        provider.rows = (1L..100L).map(::VideoRow)
        successfulScan()
        provider.queries.clear()
        provider.rows = (201L..300L).map(::VideoRow)

        val after = successfulScan()
        assertEquals((300L downTo 201L).toList(), after.map { it.id })
        assertEquals(2, provider.queries.size)
        assertEquals("date_added DESC", provider.queries.last().sortOrder)
        assertEquals(null, provider.queries.last().selection)
        provider.queries.forEach(::assertProjectionMatchesPlatform)
    }

    @Test
    fun missingPermissionStillProducesPermissionDeniedWithoutQueryingMedia() {
        shadowOf(app).denyPermissions(*MediaLibraryPermissionPolicy.requiredPermissions())
        assertEquals(VideoScanOutcome.PermissionDenied, scan())
        assertTrue(provider.queries.isEmpty())
    }

    @Test
    fun genuineProviderFailuresRemainErrorsInsteadOfLookingLikeAnEmptyLibrary() {
        provider.failure = IllegalStateException("provider unavailable")
        assertEquals(VideoScanOutcome.Error("provider unavailable"), scan())
    }

    private fun scan(): VideoScanOutcome = runBlocking {
        withTimeout(15_000) {
            scanner.scanVideos().first { it !is VideoScanOutcome.Progress }
        }
    }

    private fun successfulScan(): List<VideoItem> {
        val outcome = scan()
        assertTrue("Expected a successful scan, received $outcome", outcome is VideoScanOutcome.Success)
        return (outcome as VideoScanOutcome.Success).videos
    }

    private fun assertProjectionMatchesPlatform(query: MediaQuery) {
        if (Build.VERSION.SDK_INT < 29) {
            assertTrue("Legacy media must use the file path", "_data" in query.projection)
            assertFalse("Android 9 video views have no orientation column", "orientation" in query.projection)
            assertFalse("Relative paths require Android 10", "relative_path" in query.projection)
        } else {
            assertTrue("Modern rotation metadata must remain available", "orientation" in query.projection)
            assertTrue("Scoped storage must keep relative paths", "relative_path" in query.projection)
            assertFalse("Modern scanning must not switch back to raw file paths", "_data" in query.projection)
        }
    }

    private fun assertVideoMetadata(video: VideoItem, row: VideoRow) {
        val modern = Build.VERSION.SDK_INT >= 29
        assertEquals(row.displayName, video.title)
        assertEquals(row.duration, video.duration)
        assertEquals(row.size, video.size)
        assertEquals(row.dateAdded, video.dateAdded)
        assertEquals(row.dateModified, video.dateModified)
        assertEquals(if (modern) row.orientation else 0, video.orientationDegrees)
        assertEquals(if (modern) 1080 else 1920, video.width)
        assertEquals(if (modern) 1920 else 1080, video.height)
        assertEquals("content://media/external/video/media/${row.id}", video.uri.toString())
        assertEquals(video.uri, video.thumbnailUri)
        assertEquals(if (modern) video.uri.toString() else row.dataPath, video.path)
        assertTrue(video.libraryPath.endsWith("/Movies/ScannerRegression/${row.displayName}"))
    }
}

private data class VideoRow(
    val id: Long,
    val duration: Long = 1_000,
    val size: Long = 4_096,
    val orientation: Int = 90
) {
    val displayName: String get() = "video-$id.mp4"
    val dataPath: String get() = "/storage/emulated/0/Movies/ScannerRegression/$displayName"
    val dateAdded: Long get() = 10_000 + id
    val dateModified: Long get() = 20_000 + id
}

private data class MediaQuery(
    val projection: List<String>,
    val selection: String?,
    val selectionArgs: List<String>,
    val sortOrder: String?
)

/** Enforces the AOSP video-view schema, including rejection of unknown projection columns. */
private class VideoMediaProvider : ContentProvider() {
    @Volatile var rows: List<VideoRow> = emptyList()
    @Volatile var failure: RuntimeException? = null
    val queries: MutableList<MediaQuery> = Collections.synchronizedList(mutableListOf())

    override fun onCreate(): Boolean = true
    override fun getType(uri: Uri): String = "video/mp4"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val columns = requireNotNull(projection)
        queries += MediaQuery(columns.toList(), selection, selectionArgs?.toList().orEmpty(), sortOrder)
        failure?.let { throw it }
        val commonColumns = setOf("_id", "_display_name", "duration", "_size", "width", "height", "date_added", "date_modified")
        val available = commonColumns + if (Build.VERSION.SDK_INT < 29) setOf("_data") else setOf("relative_path", "orientation")
        columns.forEach { require(it in available) { "Invalid column $it" } }
        val selectedIds = selectionArgs?.map(String::toLong)?.toSet()
        val selected = rows.filter { selectedIds == null || it.id in selectedIds }.let {
            if (sortOrder == "date_added DESC") it.sortedByDescending(VideoRow::dateAdded) else it
        }
        return MatrixCursor(columns).apply {
            selected.forEach { video ->
                addRow(columns.map { column ->
                    when (column) {
                        "_id" -> video.id
                        "_display_name" -> video.displayName
                        "_data" -> video.dataPath
                        "relative_path" -> "Movies/ScannerRegression/"
                        "duration" -> video.duration
                        "_size" -> video.size
                        "width" -> 1920
                        "height" -> 1080
                        "date_added" -> video.dateAdded
                        "date_modified" -> video.dateModified
                        "orientation" -> video.orientation
                        else -> error("Unexpected column $column")
                    }
                }.toTypedArray<Any>())
            }
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
