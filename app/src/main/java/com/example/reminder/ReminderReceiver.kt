package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.repository.DailyLifeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "dlm_reminders_channel"
        const val CHANNEL_NAME = "Daily Life Routine Reminders"
        const val ACTION_REMINDER = "com.example.dailylifemanager.ACTION_REMINDER"
        const val ACTION_COMPLETE_TASK = "com.example.dailylifemanager.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.example.dailylifemanager.ACTION_SNOOZE_TASK"

        const val EXTRA_ACTIVITY_ID = "extra_activity_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val activityId = intent.getLongExtra(EXTRA_ACTIVITY_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Daily Activity Reminder"
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Routine"
        val soundUriStr = intent.getStringExtra(EXTRA_SOUND_URI) ?: ""

        when (intent.action) {
            ACTION_COMPLETE_TASK -> {
                if (activityId != -1L) {
                    val repository = DailyLifeRepository(AppDatabase.getDatabase(context))
                    CoroutineScope(Dispatchers.IO).launch {
                        val activity = AppDatabase.getDatabase(context).activityDao().getActivityById(activityId)
                        if (activity != null) {
                            repository.toggleActivityCompletion(activity)
                        }
                    }
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(activityId.toInt())
            }

            ACTION_SNOOZE_TASK -> {
                val reminderManager = ReminderManager(context)
                reminderManager.scheduleSnooze(activityId, title, category, 10, soundUriStr)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(activityId.toInt())
            }

            else -> {
                showNotification(context, activityId, title, category, soundUriStr)
            }
        }
    }

    private fun showNotification(context: Context, activityId: Long, title: String, category: String, soundUriStr: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri: Uri = if (soundUriStr.isNotBlank()) {
            try {
                Uri.parse(soundUriStr)
            } catch (e: Exception) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .build()

        val channelId = if (soundUriStr.isNotBlank()) {
            "dlm_reminders_channel_${Math.abs(soundUriStr.hashCode())}"
        } else {
            CHANNEL_ID
        }

        // Create Channel on Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled daily routines, hydration, study, and workouts."
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Play custom audio directly as well to ensure immediate sound playback
        try {
            var playerStarted = false
            try {
                val mp = MediaPlayer()
                mp.setDataSource(context, soundUri)
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                mp.prepare()
                mp.start()
                playerStarted = true
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        if (mp.isPlaying) mp.stop()
                        mp.release()
                    } catch (e: Exception) {}
                }, 15000L)
            } catch (e: Exception) {
                // Fallback to ringtone
            }

            if (!playerStarted) {
                val ringtone = RingtoneManager.getRingtone(context, soundUri)
                ringtone?.play()
            }
        } catch (e: Exception) {
            // Ignore ringtone playback failure
        }

        // Tap opens MainActivity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            activityId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Complete
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_ACTIVITY_ID, activityId)
            putExtra(EXTRA_TITLE, title)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (activityId + 10000).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_TASK
            putExtra(EXTRA_ACTIVITY_ID, activityId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CATEGORY, category)
            putExtra(EXTRA_SOUND_URI, soundUriStr)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (activityId + 20000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Time for your scheduled $category session!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setSound(soundUri)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze 10m", snoozePendingIntent)
            .build()

        notificationManager.notify(if (activityId > 0) activityId.toInt() else 1001, notification)
    }
}
