package com.message.sms.texting.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.Locale

object MiuiUtils {

    fun isMiui(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        val miuiBrands = listOf("xiaomi", "redmi", "poco", "blackshark")
        return manufacturer in miuiBrands || brand in miuiBrands
    }

    fun openMiuiSpecificPermissions(context: Context) {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
            intent.setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            intent.putExtra("extra_pkgname", context.packageName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(context)
        }
    }

    fun openMiuiAutoStart(context: Context) {
        try {
            val intent = Intent()
            intent.setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(context)
        }
    }
    
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isMiuiBackgroundPopupGranted(context: Context): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val method = android.app.AppOpsManager::class.java.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            val popupResult = method.invoke(appOpsManager, 10021, android.os.Process.myUid(), context.packageName) as Int
            val lockScreenResult = method.invoke(appOpsManager, 10020, android.os.Process.myUid(), context.packageName) as Int
            popupResult == android.app.AppOpsManager.MODE_ALLOWED && lockScreenResult == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            true // If we can't check, assume true to not block the user forever
        }
    }

    fun isMiuiAutostartGranted(context: Context): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val method = android.app.AppOpsManager::class.java.getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            val result = method.invoke(appOpsManager, 10008, android.os.Process.myUid(), context.packageName) as Int
            result == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            true // fallback
        }
    }
}
