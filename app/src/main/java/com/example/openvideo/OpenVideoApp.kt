package com.example.openvideo

import android.app.Application
import com.example.openvideo.core.diagnostics.CrashLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OpenVideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
