package com.message.sms.texting.app.utils

import android.os.Build
import java.util.Locale

/** Vivo (FuntouchOS/OriginOS, plus its iQOO sub-brand) has its own background-autostart
 * restriction, same category of problem as [MiuiUtils]/[OnePlusUtils], just Vivo's own settings
 * surface for it -- the report this was built against explicitly calls out Vivo alongside
 * Xiaomi/Oppo as a brand that needs this ask. */
object VivoUtils {
    fun isVivo(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        val brands = listOf("vivo", "iqoo")
        return manufacturer in brands || brand in brands
    }
}
