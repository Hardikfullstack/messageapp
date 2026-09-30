package com.message.sms.texting.app.utils

import android.content.Context
import android.os.Build
import android.provider.Telephony

/** Whether onboarding + language + default-SMS are done â€” the same "fully set up" check Splash
 * uses to decide when it's safe to route to Dashboard. Also used to gate ads that shouldn't
 * interrupt first-run setup (e.g. App Open on background return).
 *
 * Notification permission and the After Call permissions (Overlay/Battery/MIUI+OnePlus
 * autostart) are deliberately NOT part of this check anymore -- notification is asked in context
 * on the inbox itself, not during setup, and After Call is now opt-in from Settings, not part of
 * first launch. A user who never touches After Call should never have "fully set up" permanently
 * read false because of it. */
object SetupState {
    fun isFullySetUp(context: Context): Boolean {
        val prefs = AppPreferences(context)
        val isCoreSetupDone = prefs.onboardingCompleted && prefs.languageSelected

        val isDefaultSms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
            roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
        AnalyticsManager.setUserProperty("is_default_sms_app", if (isDefaultSms) "yes" else "no")

        return isCoreSetupDone && isDefaultSms
    }
}
