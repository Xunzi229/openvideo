package com.example.openvideo.core.diagnostics

/** Keeps one narrowly identified OEM helper-thread bug from terminating the app process. */
object UncaughtExceptionPolicy {

    fun shouldIsolateFromProcessTermination(threadName: String, throwable: Throwable): Boolean {
        if (threadName != ZTE_GAME_THREAD || throwable !is NullPointerException) return false
        val hasVendorFrame = throwable.stackTrace.any { frame ->
            frame.className == ZTE_GAME_STUB_CLASS && frame.methodName == ZTE_UNREGISTER_METHOD
        }
        val hasObserverFailure = throwable.message?.contains(ZTE_KEY_OBSERVER_METHOD) == true
        return hasVendorFrame && hasObserverFailure
    }

    private const val ZTE_GAME_THREAD = "GameActivityStub"
    private const val ZTE_GAME_STUB_CLASS = "com.zte.gameassist.app.GameActivityStub"
    private const val ZTE_UNREGISTER_METHOD = "unregisterObserver"
    private const val ZTE_KEY_OBSERVER_METHOD = "android.view.KeyMapObserver.stop"
}
