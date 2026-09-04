package com.example.openvideo.core.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UncaughtExceptionPolicyTest {

    @Test
    fun exactZteGameAssistObserverFailureIsIsolated() {
        assertTrue(
            UncaughtExceptionPolicy.shouldIsolateFromProcessTermination(
                threadName = "GameActivityStub",
                throwable = zteObserverFailure()
            )
        )
    }

    @Test
    fun vendorFailureOnMainThreadIsNeverIsolated() {
        assertFalse(
            UncaughtExceptionPolicy.shouldIsolateFromProcessTermination(
                threadName = "main",
                throwable = zteObserverFailure()
            )
        )
    }

    @Test
    fun unrelatedNullPointerOnVendorNamedThreadIsNeverIsolated() {
        assertFalse(
            UncaughtExceptionPolicy.shouldIsolateFromProcessTermination(
                threadName = "GameActivityStub",
                throwable = NullPointerException("app failure")
            )
        )
    }

    @Test
    fun observerMessageWithoutVendorStackIsNeverIsolated() {
        assertFalse(
            UncaughtExceptionPolicy.shouldIsolateFromProcessTermination(
                threadName = "GameActivityStub",
                throwable = NullPointerException(
                    "Attempt to invoke android.view.KeyMapObserver.stop on a null object reference"
                )
            )
        )
    }

    private fun zteObserverFailure(): NullPointerException = NullPointerException(
        "Attempt to invoke android.view.KeyMapObserver.stop on a null object reference"
    ).apply {
        stackTrace = arrayOf(
            StackTraceElement(
                "com.zte.gameassist.app.GameActivityStub",
                "unregisterObserver",
                "GameActivityStub.java",
                137
            )
        )
    }
}
