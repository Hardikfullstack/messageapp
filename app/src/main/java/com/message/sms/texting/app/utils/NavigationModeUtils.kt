package com.message.sms.texting.app.utils

import android.content.Context

/** True if this device is set to full gesture navigation (edge-swipe, no visible nav buttons) --
 * value 2 of AOSP's internal navBarInteractionMode (0 = 3-button, 1 = old 2-button gesture,
 * 2 = full gesture). Used to skip explicitly hiding the navigation bar: doing so has been
 * observed to also disable the OS's own edge-swipe back gesture entirely on some gesture-nav OEM
 * skins (OxygenOS/ColorOS) -- the bar being "hidden" and the gesture-recognition zone are
 * apparently the same thing to their implementation. */
fun isGestureNavigationEnabled(context: Context): Boolean {
    return try {
        val resourceId = context.resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        resourceId > 0 && context.resources.getInteger(resourceId) == 2
    } catch (e: Exception) {
        false
    }
}
