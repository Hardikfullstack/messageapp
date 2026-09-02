package com.message.sms.texting.app.utils

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.message.sms.texting.app.R

private const val TAG = "AfterCallMiniOverlay"

object AfterCallMiniOverlay {
    private var rootView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun show(context: Context) {
        val appContext = context.applicationContext
        hide()

        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) {
            Log.e(TAG, "WindowManager unavailable")
            return
        }

        // A raw WindowManager-added ComposeView has no Lifecycle/SavedStateRegistry of its own --
        // needs one attached manually or Compose throws (see OverlayLifecycleOwner's doc comment).
        val owner = OverlayLifecycleOwner(appContext as android.app.Application)
        val composeView = ComposeView(appContext).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setContent { InvisibleCard() }
        }
        owner.attachToView(composeView)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 0
        }

        try {
            windowManager.addView(composeView, layoutParams)
            rootView = composeView
            lifecycleOwner = owner
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add mini overlay", e)
        }
    }

    fun hide() {
        val view = rootView ?: return
        rootView = null
        try {
            val windowManager = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            windowManager?.removeView(view)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove mini overlay", e)
        }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }
}

@Composable
private fun InvisibleCard() {
    Column(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colorResource(R.color.bg_primary))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(0f)
    ) {
        Text(
            text = " ",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorResource(R.color.text_title)
        )
    }
}
