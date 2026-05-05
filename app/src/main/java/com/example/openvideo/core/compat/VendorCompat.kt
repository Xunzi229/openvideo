package com.example.openvideo.core.compat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object VendorCompat {

    enum class Vendor {
        XIAOMI, HUAWEI, OPPO, VIVO, SAMSUNG, ONEPLUS, MEIZU, UNKNOWN
    }

    fun detectVendor(): Vendor {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi") -> Vendor.XIAOMI
            manufacturer.contains("huawei") || brand.contains("huawei") || brand.contains("honor") -> Vendor.HUAWEI
            manufacturer.contains("oppo") || brand.contains("oppo") || brand.contains("realme") -> Vendor.OPPO
            manufacturer.contains("vivo") || brand.contains("vivo") -> Vendor.VIVO
            manufacturer.contains("samsung") || brand.contains("samsung") -> Vendor.SAMSUNG
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> Vendor.ONEPLUS
            manufacturer.contains("meizu") || brand.contains("meizu") -> Vendor.MEIZU
            else -> Vendor.UNKNOWN
        }
    }

    fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        return fingerprint.contains("generic") || fingerprint.contains("sdk") ||
                fingerprint.contains("emulator") || Build.MODEL.lowercase().contains("emulator")
    }

    fun needsBackgroundPermissionGuide(): Boolean {
        return when (detectVendor()) {
            Vendor.XIAOMI, Vendor.HUAWEI, Vendor.OPPO, Vendor.VIVO, Vendor.ONEPLUS, Vendor.MEIZU -> true
            Vendor.SAMSUNG, Vendor.UNKNOWN -> false
        }
    }

    fun getBackgroundSettingsIntent(context: Context): Intent? {
        return when (detectVendor()) {
            Vendor.XIAOMI -> {
                try {
                    Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                        setClassName("com.miui.securitycenter",
                            "com.miui.permcenter.permissions.PermissionsEditorActivity")
                       .putExtra("extra_pkgname", context.packageName)
                    }
                } catch (_: Exception) {
                    getAppDetailsIntent(context)
                }
            }
            Vendor.HUAWEI -> {
                try {
                    Intent().apply {
                        setClassName("com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                    }
                } catch (_: Exception) {
                    getAppDetailsIntent(context)
                }
            }
            Vendor.OPPO, Vendor.ONEPLUS -> {
                try {
                    Intent("oppo.intent.action.AUTO_START").apply {
                        setPackage("com.coloros.safecenter")
                    }
                } catch (_: Exception) {
                    getAppDetailsIntent(context)
                }
            }
            Vendor.VIVO -> {
                try {
                    Intent().apply {
                        setClassName("com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                    }
                } catch (_: Exception) {
                    getAppDetailsIntent(context)
                }
            }
            else -> getAppDetailsIntent(context)
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to app details
            activity.startActivity(getAppDetailsIntent(activity))
        }
    }

    private fun getAppDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
