package com.messages.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.core.content.ContextCompat

/** Whether onboarding + all permissions + default-SMS are done — the same "fully set up" check
 * Splash uses to decide when it's safe to route to Dashboard. Also used to gate ads that
 * shouldn't interrupt the onboarding/permission-granting flow (e.g. App Open on background
 * return, while the user is bouncing to system Settings for MIUI/overlay/alarm permissions). */
object SetupState {
    fun isFullySetUp(context: Context): Boolean {
        val hasNotif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        val hasPhone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        val prefs = AppPreferences(context)
        val isFullyOnboarded = prefs.onboardingCompleted && hasNotif && hasPhone
        val isPermissionsDone = isFullyOnboarded && Settings.canDrawOverlays(context) &&
                (!MiuiUtils.isMiui() || (prefs.miuiPermissionsCompleted && prefs.miuiAutostartCompleted)) &&
                (!OnePlusUtils.isOnePlus() || prefs.onePlusAutostartCompleted)

        val isDefaultSms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
            roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
        AnalyticsManager.setUserProperty("is_default_sms_app", if (isDefaultSms) "yes" else "no")

        return isPermissionsDone && isDefaultSms
    }
}
