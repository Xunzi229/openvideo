package com.openvideo.app.core.prefs

import android.app.Application
import android.content.Context
import android.os.Looper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28], application = Application::class)
class LocalContinuePlaybackPositionTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun defaultsAndUnknownStoredValuesKeepTheExistingTopPosition() {
        assertEquals(LocalContinuePlaybackPosition.TOP, AppPrefs(context).localContinuePlaybackPosition)
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .edit().putString("local_continue_playback_position", "unknown").commit()
        assertEquals(LocalContinuePlaybackPosition.TOP, AppPrefs(context).localContinuePlaybackPosition)
    }

    @Test
    fun positionSurvivesNewPreferenceInstancesAndCanBeChangedBack() {
        AppPrefs(context).localContinuePlaybackPosition = LocalContinuePlaybackPosition.BOTTOM
        assertEquals(LocalContinuePlaybackPosition.BOTTOM, AppPrefs(context).localContinuePlaybackPosition)
        AppPrefs(context).localContinuePlaybackPosition = LocalContinuePlaybackPosition.TOP
        assertEquals(LocalContinuePlaybackPosition.TOP, AppPrefs(context).localContinuePlaybackPosition)
    }

    @Test
    fun activeLocalTabReceivesSettingsChangesWithoutBeingRecreated() = runBlocking {
        withTimeout(5_000) {
            val localTabPrefs = AppPrefs(context)
            val settingsPrefs = AppPrefs(context)
            val received = mutableListOf<LocalContinuePlaybackPosition>()
            val subscribed = CompletableDeferred<Unit>()
            val collector = launch {
                localTabPrefs.observeLocalContinuePlaybackPosition().take(2).collect {
                    received += it
                    subscribed.complete(Unit)
                }
            }
            subscribed.await()
            settingsPrefs.localContinuePlaybackPosition = LocalContinuePlaybackPosition.BOTTOM
            shadowOf(Looper.getMainLooper()).idle()
            collector.join()
            assertEquals(listOf(LocalContinuePlaybackPosition.TOP, LocalContinuePlaybackPosition.BOTTOM), received)
        }
    }
}
