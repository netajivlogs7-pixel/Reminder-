package com.example.util

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns

data class AudioPreset(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val uriString: String
)

object AudioUtils {

    /**
     * Resolves human-readable name of an audio Uri (from document storage, media store, or ringtones)
     */
    fun getAudioFileName(context: Context, uri: Uri): String {
        var displayName = ""

        // 1. Try content resolver query for OpenableColumns.DISPLAY_NAME (files from storage / file picker)
        if (uri.scheme == "content") {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            displayName = it.getString(nameIndex) ?: ""
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore cursor query exception
            }
        }

        // 2. Try RingtoneManager to get ringtone/system sound title
        if (displayName.isBlank()) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val title = ringtone?.getTitle(context)
                if (!title.isNullOrBlank()) {
                    displayName = title
                }
            } catch (e: Exception) {
                // Ignore ringtone title exception
            }
        }

        // 3. Fallback to last path segment or generic title
        if (displayName.isBlank()) {
            val segment = uri.lastPathSegment
            displayName = if (!segment.isNullOrBlank()) {
                segment.substringAfterLast("/")
            } else {
                "Custom Audio File"
            }
        }

        return displayName
    }

    /**
     * Safely takes persistable URI permission for audio files chosen via Storage Picker
     */
    fun takePersistableUriPermissionSafely(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Some providers (or ringtone picker) do not support persistable permissions
        }
    }

    /**
     * Standard audio presets available in the app
     */
    fun getPresets(): List<AudioPreset> {
        val alarmUri = try {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: ""
        } catch (e: Exception) { "" }

        val ringtoneUri = try {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.toString() ?: ""
        } catch (e: Exception) { "" }

        val notificationUri = try {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString() ?: ""
        } catch (e: Exception) { "" }

        return listOf(
            AudioPreset("default", "Default Alert", "डिफ़ॉल्ट अलर्ट", ""),
            AudioPreset("alarm", "Alarm Bell", "अलार्म घंटी", alarmUri),
            AudioPreset("ringtone", "Phone Ringtone", "फ़ोन रिंगटोन", ringtoneUri),
            AudioPreset("chime", "Notification Chime", "नोटिफिकेशन चाइम", notificationUri)
        )
    }
}
