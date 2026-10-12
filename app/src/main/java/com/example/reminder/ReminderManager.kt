package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.ActivityEntity
import java.util.Calendar

class ReminderManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleReminder(activity: ActivityEntity, defaultSoundUri: String = "") {
        if (alarmManager == null) return

        try {
            val parts = activity.startTime.split(":")
            if (parts.size != 2) return
            val hour = parts[0].toIntOrNull() ?: return
            val minute = parts[1].toIntOrNull() ?: return

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // subtract reminder offset
                add(Calendar.MINUTE, -activity.reminderMinutesBefore)
            }

            // If time has passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val soundToUse = if (activity.customSoundUri.isNotBlank()) activity.customSoundUri else defaultSoundUri

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderReceiver.ACTION_REMINDER
                putExtra(ReminderReceiver.EXTRA_ACTIVITY_ID, activity.id)
                putExtra(ReminderReceiver.EXTRA_TITLE, activity.title)
                putExtra(ReminderReceiver.EXTRA_CATEGORY, activity.category)
                putExtra(ReminderReceiver.EXTRA_SOUND_URI, soundToUse)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                activity.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = calendar.timeInMillis
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to schedule reminder: ${e.message}")
        }
    }

    fun scheduleSnooze(activityId: Long, title: String, category: String, minutes: Int, soundUri: String = "") {
        if (alarmManager == null) return
        val triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_ACTIVITY_ID, activityId)
            putExtra(ReminderReceiver.EXTRA_TITLE, "$title (Snoozed)")
            putExtra(ReminderReceiver.EXTRA_CATEGORY, category)
            putExtra(ReminderReceiver.EXTRA_SOUND_URI, soundUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (activityId + 30000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("ReminderManager", "Failed to snooze: ${e.message}")
        }
    }

    fun cancelReminder(activityId: Long) {
        if (alarmManager == null) return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            activityId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun sendTestNotification(
        title: String = "Daily Life Manager",
        message: String = "Test reminder working properly!",
        soundUri: String = ""
    ) {
        val receiver = ReminderReceiver()
        val testIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_ACTIVITY_ID, 9999L)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_CATEGORY, message)
            putExtra(ReminderReceiver.EXTRA_SOUND_URI, soundUri)
        }
        receiver.onReceive(context, testIntent)
    }
}
