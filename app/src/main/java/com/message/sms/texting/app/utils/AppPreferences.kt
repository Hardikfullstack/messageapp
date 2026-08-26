package com.message.sms.texting.app.utils

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    var languageSelected: Boolean
        get() = prefs.getBoolean("language_selected", false)
        set(value) = prefs.edit().putBoolean("language_selected", value).apply()

    var overlayPermissionAutoPrompted: Boolean
        get() = prefs.getBoolean("overlay_permission_auto_prompted", false)
        set(value) = prefs.edit().putBoolean("overlay_permission_auto_prompted", value).apply()

    var miuiPermissionsCompleted: Boolean
        get() = prefs.getBoolean("miui_permissions_completed", false)
        set(value) = prefs.edit().putBoolean("miui_permissions_completed", value).apply()

    var miuiAutostartCompleted: Boolean
        get() = prefs.getBoolean("miui_autostart_completed", false)
        set(value) = prefs.edit().putBoolean("miui_autostart_completed", value).apply()

    // Battery optimization is a one-time ask (not a hard gate â€” Play Store restricts requiring
    // REQUEST_IGNORE_BATTERY_OPTIMIZATIONS as mandatory), so this just tracks "already asked once".
    var batteryOptimizationCompleted: Boolean
        get() = prefs.getBoolean("battery_optimization_completed", false)
        set(value) = prefs.edit().putBoolean("battery_optimization_completed", value).apply()

    var onePlusAutostartCompleted: Boolean
        get() = prefs.getBoolean("oneplus_autostart_completed", false)
        set(value) = prefs.edit().putBoolean("oneplus_autostart_completed", value).apply()
}
