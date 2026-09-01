package com.message.sms.texting.app.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Locale

/** OxygenOS has its own "Auto-launch" background-start toggle (off by default for third-party
 * apps), separate from and layered on top of the standard Android overlay/battery permissions â€”
 * same category of problem as [MiuiUtils], just OnePlus's own settings surface for it.
 * Also matches "oppo" and "realme" â€” post the OnePlus/Oppo merger, OxygenOS 12+ runs on the same
 * ColorOS-based security app/components, and Realme UI is built on that same ColorOS base (Realme
 * spun off from Oppo), so both have the identical restriction and fix. */
object OnePlusUtils {
    fun isOnePlus(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        val brands = listOf("oneplus", "oppo", "realme")
        return manufacturer in brands || brand in brands
    }

    /** Best-effort, undocumented components (community-known, not officially published) â€” covers
     * both older stock OxygenOS and newer ColorOS-based OxygenOS 12+/Oppo builds, tried in order;
     * falls back to the app's own Settings page (same fallback [MiuiUtils] uses) if none resolve. */
    fun openOnePlusAutoLaunch(context: Context) {
        val candidates = listOf(
            // Older OxygenOS (pre-ColorOS merge, roughly Android 11 and below).
            "com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
            // Newer OxygenOS 12+ / Oppo, ColorOS-based.
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.coloros.bootreg" to "com.coloros.bootreg.activity.MainActivity",
            "com.oplus.safecenter" to "com.oplus.safecenter.startupapp.StartupAppListActivity"
        )
        for ((pkg, cls) in candidates) {
            try {
                val intent = Intent().apply {
                    setClassName(pkg, cls)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Try the next candidate.
            }
        }
        MiuiUtils.openAppSettings(context)
    }
}
