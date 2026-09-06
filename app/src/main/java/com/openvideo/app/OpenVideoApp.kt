package com.openvideo.app

import android.app.Application
import com.openvideo.app.core.diagnostics.CrashLogger
import com.openvideo.app.core.prefs.AppPrefs
import com.openvideo.app.ui.settings.AppSettingsApplier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OpenVideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppSettingsApplier.apply(AppPrefs(this))
        CrashLogger.install(this)
        CrashLogger.flushPendingReports(this)
    }
}
