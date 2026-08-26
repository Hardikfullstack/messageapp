package com.message.sms.texting.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.message.sms.texting.app.receiver.AutoDeleteReceiver
import com.message.sms.texting.app.receiver.ReminderReceiver
import com.message.sms.texting.app.receiver.ScheduledMessageReceiver

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val AUTO_DELETE_REQUEST_CODE = 9001
    }

    private fun autoDeletePendingIntent(): PendingIntent {
        val intent = Intent(context, AutoDeleteReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            AUTO_DELETE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Schedules a one-shot check ~24h from now; the receiver reschedules itself each time it runs. */
    fun scheduleAutoDeleteCheck() {
        val triggerAt = System.currentTimeMillis() + AlarmManager.INTERVAL_DAY
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, autoDeletePendingIntent())
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, autoDeletePendingIntent())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelAutoDeleteCheck() {
        alarmManager.cancel(autoDeletePendingIntent())
    }

    /** True if an auto-delete alarm is currently armed (survives across app-open calls; false after a reboot, since alarms don't persist). */
    fun isAutoDeleteScheduled(): Boolean {
        val intent = Intent(context, AutoDeleteReceiver::class.java)
        val existing = PendingIntent.getBroadcast(
            context,
            AUTO_DELETE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return existing != null
    }

    fun scheduleMessage(scheduledMessageId: Long, timeInMillis: Long) {
        val intent = Intent(context, ScheduledMessageReceiver::class.java).apply {
            putExtra(ScheduledMessageReceiver.EXTRA_MESSAGE_ID, scheduledMessageId)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduledMessageId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setAlarmClock to completely bypass Doze mode delays for exact precision
                val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // This happens if SCHEDULE_EXACT_ALARM is not granted. 
            // We should ideally prevent reaching this point via UI checks.
        }
    }

    fun cancelMessage(scheduledMessageId: Long) {
        val intent = Intent(context, ScheduledMessageReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduledMessageId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun reminderPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * A "call back later" reminder uses exact precision to prevent delays from Doze mode.
     */
    fun scheduleReminder(reminderId: Long, timeInMillis: Long) {
        val pendingIntent = reminderPendingIntent(reminderId)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Fallback if exact alarm permission is denied (prevents dropping the alarm completely)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
            }
        }
    }

    fun cancelReminder(reminderId: Long) {
        alarmManager.cancel(reminderPendingIntent(reminderId))
    }
}
