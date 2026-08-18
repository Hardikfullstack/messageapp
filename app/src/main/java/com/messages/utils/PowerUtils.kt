package com.messages.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import java.util.Locale

object PowerUtils {
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun ignoreBatteryOptimizationsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
    }

    /** Only worth asking on OEMs that (a) don't already have their own dedicated permission
     * covering the same need — MIUI's Autostart step already does — and (b) are known to actually
     * need it — Samsung/stock-Android-family devices reliably run background receivers/overlays
     * without it, so asking there is pure friction with no benefit. */
    fun shouldPromptForBatteryOptimization(): Boolean {
        if (MiuiUtils.isMiui()) return false
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        val skipBrands = listOf("samsung", "google")
        return manufacturer !in skipBrands && brand !in skipBrands
    }
}
