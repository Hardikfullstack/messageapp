package com.messages.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Locale

/** OxygenOS has its own "Auto-launch" background-start toggle (off by default for third-party
 * apps), separate from and layered on top of the standard Android overlay/battery permissions —
 * same category of problem as [MiuiUtils], just OnePlus's own settings surface for it. */
object OnePlusUtils {
    fun isOnePlus(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        return manufacturer == "oneplus" || brand == "oneplus"
    }

    /** Best-effort, undocumented component (community-known, not officially published by OnePlus)
     * — varies across OxygenOS versions, so this can fail; falls back to the app's own Settings
     * page (same fallback [MiuiUtils] uses) rather than leaving the user stuck. */
    fun openOnePlusAutoLaunch(context: Context) {
        try {
            val intent = Intent().apply {
                setClassName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            MiuiUtils.openAppSettings(context)
        }
    }
}
