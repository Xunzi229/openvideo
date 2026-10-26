package com.openvideo.app.core.permissions

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LegacyMediaWritePermissionTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val registry = RecordingRegistry()
    private var activeContext: Context? = null

    private fun permission(): LegacyMediaWritePermission {
        activeContext = app
        return LegacyMediaWritePermission(RegistryCaller(registry)) { activeContext }
    }

    @Test fun registrationDoesNotRequestPermissionUntilAnOperation() {
        permission()
        assertEquals(0, registry.launches)
        assertTrue(app.packageManager.getPackageInfo(app.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    @Test fun readPermissionAloneDoesNotAuthorizeWritesAndGrantRunsTheOriginalOperationOnce() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
        shadowOf(app).denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission = permission()
        var writes = 0
        permission.runWhenGranted { writes++ }
        assertEquals(0, writes)
        assertEquals(Manifest.permission.WRITE_EXTERNAL_STORAGE, registry.lastInput)
        registry.dispatchResult(registry.lastRequestCode, true)
        registry.dispatchResult(registry.lastRequestCode, true)
        assertEquals(1, writes)
    }

    @Test fun denialDoesNotWriteAndRetryDoesNotReplayTheDeniedOperation() {
        val permission = permission()
        var oldWrites = 0
        var retryWrites = 0
        permission.runWhenGranted { oldWrites++ }
        registry.dispatchResult(registry.lastRequestCode, false)
        assertNotNull(ShadowToast.getTextOfLatestToast())
        assertEquals(0, oldWrites)
        permission.runWhenGranted { retryWrites++ }
        registry.dispatchResult(registry.lastRequestCode, true)
        assertEquals(0, oldWrites)
        assertEquals(1, retryWrites)
    }

    @Test fun repeatedClicksDuringPermissionPromptDoNotQueueMultipleDestructiveOperations() {
        val permission = permission()
        var writes = 0
        permission.runWhenGranted { writes++ }
        permission.runWhenGranted { writes += 100 }
        assertEquals(1, registry.launches)
        registry.dispatchResult(registry.lastRequestCode, true)
        assertEquals(1, writes)
    }

    @Test fun alreadyGrantedPermissionRunsWithoutPrompting() {
        shadowOf(app).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission = permission()
        var writes = 0
        permission.runWhenGranted { writes++ }
        assertEquals(1, writes)
        assertEquals(0, registry.launches)
    }

    @Test fun detachedOwnerDoesNotWriteOnPermissionResult() {
        val permission = permission()
        var writes = 0
        permission.runWhenGranted { writes++ }
        activeContext = null
        registry.dispatchResult(registry.lastRequestCode, true)
        assertEquals(0, writes)
    }

    @Test
    @Config(sdk = [29, 33])
    fun android10AndAboveKeepTheirExistingStorageFlowWithoutLegacyPermission() {
        shadowOf(app).denyPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission = permission()
        var writes = 0
        permission.runWhenGranted { writes++ }
        assertEquals(1, writes)
        assertEquals(0, registry.launches)
    }

    private class RecordingRegistry : ActivityResultRegistry() {
        var launches = 0
        var lastRequestCode = 0
        var lastInput: Any? = null
        override fun <I, O> onLaunch(
            requestCode: Int, contract: ActivityResultContract<I, O>, input: I,
            options: ActivityOptionsCompat?
        ) {
            launches++
            lastRequestCode = requestCode
            lastInput = input
        }
    }

    private class RegistryCaller(private val registry: ActivityResultRegistry) : ActivityResultCaller {
        override fun <I, O> registerForActivityResult(
            contract: ActivityResultContract<I, O>, callback: ActivityResultCallback<O>
        ): ActivityResultLauncher<I> = registry.register("legacy-write", contract, callback)

        override fun <I, O> registerForActivityResult(
            contract: ActivityResultContract<I, O>, registry: ActivityResultRegistry,
            callback: ActivityResultCallback<O>
        ): ActivityResultLauncher<I> = registry.register("legacy-write", contract, callback)
    }
}
