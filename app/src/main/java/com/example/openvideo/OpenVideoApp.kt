package com.example.openvideo

import android.app.Application
import com.example.openvideo.core.diagnostics.CrashLogger
import com.example.openvideo.core.prefs.AppPrefs
import com.example.openvideo.ui.settings.AppSettingsApplier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OpenVideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppSettingsApplier.apply(AppPrefs(this))
        CrashLogger.install(this)
    }
}
