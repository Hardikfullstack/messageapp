package com.message.sms.texting.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.PowerManager
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.message.sms.texting.app.receiver.NotificationActionReceiver
import com.message.sms.texting.app.ui.theme.NotificationSettingsState

object AppState {
    var activeThreadId: Long? = null
    var activeAddress: String? = null
}

object NotificationHelper {

    /** One shared channel for all SMS notifications (not per-contact) â€” matches how most messaging
     * apps structure this, and lets Settings â†’ Notification deep-link to a single, clean channel
     * screen instead of a list of many per-contact channels. */
    const val DEFAULT_CHANNEL_ID = "default_sms_channel"

    /** Creates the shared channel if it doesn't exist yet. Importance is only set at creation â€”
     * once the channel exists we never touch it again, since the user may have customized it
     * themselves via system Settings, and that must not be silently overwritten later. */
    fun ensureDefaultChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID) == null) {
            val quickReplyEnabled = NotificationSettingsState.readQuickReplyEnabled(context)
            val desiredImportance = if (quickReplyEnabled) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                context.getString(R.string.default_notification_channel_name),
                desiredImportance
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** Separate small channel for After Call "call back" reminders â€” distinct from SMS notifications
     * since it's a different kind of prompt (an action reminder, not a new message). */
    const val REMINDER_CHANNEL_ID = "after_call_reminder_channel_v2"

    private fun ensureReminderChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(REMINDER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                context.getString(R.string.feature_after_call_title),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** Separate channel for the After Call full-screen-intent fallback (see
     * [showAfterCallFullScreenNotification]) â€” needs its own IMPORTANCE_HIGH channel, distinct
     * from the reminder one, since it carries a fullScreenIntent. */
    const val AFTER_CALL_CHANNEL_ID = "after_call_screen_channel"
    const val AFTER_CALL_NOTIFICATION_ID = 9001

    private fun ensureAfterCallChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(AFTER_CALL_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                AFTER_CALL_CHANNEL_ID,
                context.getString(R.string.feature_after_call_title),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Fallback delivery path for the After Call screen, used when a direct startActivity() from
     * AfterCallReceiver doesn't result in the screen actually appearing (see the delayed check in
     * AfterCallReceiver) â€” some OEMs (OnePlus/OxygenOS in particular) silently swallow a plain
     * background startActivity() call despite the app holding SYSTEM_ALERT_WINDOW. A full-screen
     * intent notification goes through the NotificationManager instead, which is the one
     * Android-blessed path for background-triggered full-screen UI (the same mechanism incoming
     * calls/alarms use) and is far less likely to be silently blocked. If the device is locked,
     * the system launches [contentIntent] automatically; otherwise this shows as a tappable
     * heads-up notification instead â€” a working fallback either way, instead of nothing at all.
     */
    fun showAfterCallFullScreenNotification(
        context: Context,
        contentIntent: Intent
    ) {
        ensureAfterCallChannelExists(context)

        val title = context.getString(R.string.after_call_fallback_notification_title)

        val pendingIntent = PendingIntent.getActivity(
            context, AFTER_CALL_NOTIFICATION_ID, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, AFTER_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_msg)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        NotificationManagerCompat.from(context).notify(AFTER_CALL_NOTIFICATION_ID, builder.build())
    }

    fun cancelAfterCallFullScreenNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(AFTER_CALL_NOTIFICATION_ID)
    }

    /** [foregroundRes] (e.g. logo_msg, which has a transparent background) drawn centered over a
     * solid [backgroundColorRes] circle â€” a notification largeIcon otherwise renders that
     * transparency as-is, which can look washed out against some system notification shades. */
    private fun drawableWithCircleBackground(
        context: Context,
        @DrawableRes foregroundRes: Int,
        @ColorRes backgroundColorRes: Int
    ): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, backgroundColorRes)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val inset = (size * 0.2f).toInt()
        ContextCompat.getDrawable(context, foregroundRes)?.apply {
            setBounds(inset, inset, size - inset, size - inset)
            draw(canvas)
        }
        return bitmap
    }

    /** Fired by ReminderReceiver when an After Call "remind me to call back" alarm goes off. */
    fun showCallBackNotification(context: Context, reminderId: Long, address: String, contactName: String?, note: String) {
        ensureReminderChannelExists(context)

        val label = contactName ?: address
        val title = String.format(context.getString(R.string.after_call_reminder_notification_title_template), label)
        val notificationId = reminderId.toInt()

        val hasCallPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CALL_PHONE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val callIntent = Intent(if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:$address")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = drawableWithCircleBackground(context, R.drawable.logo_msg, R.color.primary)

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_msg)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .apply { if (note.isNotBlank()) setContentText(note) }
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(
                NotificationCompat.Action.Builder(R.drawable.settings_ic_aftercall, context.getString(R.string.content_desc_call), pendingIntent).build()
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            }
        } else {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }

    fun showSmsNotification(
        context: Context,
        threadId: Long,
        address: String,
        body: String,
        contactName: String?
    ) {
        if (threadId == AppState.activeThreadId || address == AppState.activeAddress) {
            return
        }


        val previewOption = NotificationSettingsState.readPreviewOption(context, threadId)
        val wakeScreenEnabled = NotificationSettingsState.readWakeScreenEnabled(context, threadId)

        val displayContactName = contactName ?: address
        val channelId = DEFAULT_CHANNEL_ID
        ensureDefaultChannelExists(context)

        val title: String
        val text: String

        when (previewOption) {
            0 -> {
                // Show name and message
                title = displayContactName
                text = body
            }
            1 -> {
                // Show name only
                title = displayContactName
                text = "New message"
            }
            else -> {
                // Hide contents
                title = "New message"
                text = "You have a new message"
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_chat", true)
            putExtra("threadId", threadId)
            putExtra("address", address)
            putExtra("contactName", displayContactName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            threadId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo_msg)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(NotificationSettingsState.readTapToDismissEnabled(context))
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationId = threadId.toInt()

        // OTP quick-copy â€” only when the full preview is showing (previewOption 0); with the
        // preview hidden/name-only, adding a button that reveals the code would defeat the point
        // of that setting. Added first/leftmost so it's the most prominent action when present.
        if (previewOption == 0) {
            com.message.sms.texting.app.utils.extractOtpCode(body)?.let { code ->
                val copyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_COPY_OTP
                    putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(NotificationActionReceiver.EXTRA_OTP_CODE, code)
                }
                val copyPendingIntent = PendingIntent.getBroadcast(
                    context, notificationId * 10 + 9, copyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.chat_ic_long_copy,
                        context.getString(R.string.notif_action_copy_code),
                        copyPendingIntent
                    ).build()
                )
            }
        }

        // Action buttons (Mark as read / Reply / Call / Delete), per the global Notification Settings screen.
        val (button1, button2, button3) = NotificationSettingsState.readButtonActions(context)
        listOf(button1, button2, button3).forEachIndexed { slot, action ->
            buildNotificationAction(context, action, threadId, address, displayContactName, notificationId, slot)
                ?.let { builder.addAction(it) }
        }

        if (wakeScreenEnabled) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "MessagesApp:SmsWakeLock"
            )
            wakeLock.acquire(3000) // Wake for 3 seconds
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(threadId.toInt(), builder.build())
            }
        } else {
            NotificationManagerCompat.from(context).notify(threadId.toInt(), builder.build())
        }
    }

    private fun buildNotificationAction(
        context: Context,
        action: String,
        threadId: Long,
        address: String,
        contactName: String,
        notificationId: Int,
        slot: Int
    ): NotificationCompat.Action? {
        // Distinct per-thread, per-slot request codes so PendingIntents for different threads/buttons don't collide.
        val requestCode = notificationId * 10 + slot

        return when (action) {
            "mark_read" -> {
                val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                    this.action = NotificationActionReceiver.ACTION_MARK_READ
                    putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                NotificationCompat.Action.Builder(R.drawable.longpress_ic_more_mark_read, context.getString(R.string.menu_mark_as_read), pendingIntent).build()
            }
            "delete" -> {
                val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                    this.action = NotificationActionReceiver.ACTION_DELETE
                    putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                NotificationCompat.Action.Builder(R.drawable.longpress_ic_delete, context.getString(R.string.swipe_action_delete), pendingIntent).build()
            }
            "call" -> {
                // direct-call-vs-dialer decision is baked into the intent itself.
                val hasCallPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CALL_PHONE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val callIntent = Intent(if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:$address")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, requestCode, callIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                NotificationCompat.Action.Builder(R.drawable.settings_ic_aftercall, context.getString(R.string.content_desc_call), pendingIntent).build()
            }
            "reply" -> {
                val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                    this.action = NotificationActionReceiver.ACTION_REPLY
                    putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
                    putExtra(NotificationActionReceiver.EXTRA_ADDRESS, address)
                    putExtra(NotificationActionReceiver.EXTRA_CONTACT_NAME, contactName)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                // RemoteInput-carrying PendingIntents must be mutable, unlike the other action types above.
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY_TEXT)
                    .setLabel(context.getString(R.string.notif_action_reply))
                    .build()
                NotificationCompat.Action.Builder(R.drawable.chat_ic_send, context.getString(R.string.notif_action_reply), pendingIntent)
                    .addRemoteInput(remoteInput)
                    .build()
            }
            else -> null
        }
    }
}
