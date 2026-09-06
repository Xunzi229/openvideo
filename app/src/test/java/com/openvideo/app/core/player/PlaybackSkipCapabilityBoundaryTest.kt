package com.openvideo.app.core.player

import android.app.Application
import android.net.Uri
import com.openvideo.app.core.prefs.LoopMode
import com.openvideo.app.data.model.VideoItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class PlaybackSkipCapabilityBoundaryTest {
    @Test fun snapshotAndQueueLookupsExposeOnlyAvailableSkipActions() {
        val queue = (1L..3L).map { VideoItem(it, "$it", "content://media/$it", Uri.parse("content://media/$it"), 1, 1, 1, 1, 1, null) }
        assertFalse(PlaybackNotificationSkipCapabilityPolicy.fromSnapshot(null).hasAnySkip)
        for (id in listOf(0L, 1L, 2L, 3L)) for (loop in LoopMode.entries) {
            val snapshot = PlaybackNotificationCoordinator.Snapshot("content://media/$id", "title", id, "/Movies/$id.mp4",
                1, 1, queue, "token", loop, true, 0, 100)
            val capabilities = PlaybackNotificationSkipCapabilityPolicy.fromSnapshot(snapshot)
            val expectedNext = id != 0L && (id < 3 || loop == LoopMode.LIST)
            val expectedPrevious = id != 0L && (id > 1 || loop == LoopMode.LIST)
            assertEquals(expectedNext, capabilities.canSkipToNext)
            assertEquals(expectedPrevious, capabilities.canSkipToPrevious)
            assertEquals(expectedNext || expectedPrevious, capabilities.hasAnySkip)
        }
        assertFalse(PlaybackNotificationSkipCapabilityPolicy.fromQueueIndex(3, 3, LoopMode.OFF).hasAnySkip)
        assertNull(PlaybackQueueSkipPolicy.nextIndex(-1, 3))
        assertNull(PlaybackQueueSkipPolicy.previousIndex(3, 3))
    }
}
