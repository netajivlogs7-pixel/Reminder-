package com.example.util

object TimeUtils {

    /**
     * Converts a 24-hour time string "HH:mm" (e.g. "08:00", "18:30")
     * to a 12-hour formatted string with AM/PM (e.g. "08:00 AM", "06:30 PM").
     */
    fun formatTo12Hour(time24: String): String {
        val parts = time24.split(":")
        if (parts.size != 2) return time24
        val hour = parts[0].toIntOrNull() ?: return time24
        val minute = parts[1].toIntOrNull() ?: return time24
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%02d:%02d %s", hour12, minute, amPm)
    }

    /**
     * Checks whether the given 24-hour time string is in AM.
     */
    fun isAm(time24: String): Boolean {
        val hour = time24.split(":").firstOrNull()?.toIntOrNull() ?: 8
        return hour < 12
    }

    /**
     * Toggles or sets the time string to AM or PM without changing the minutes.
     */
    fun setAmPm(time24: String, targetAm: Boolean): String {
        val parts = time24.split(":")
        if (parts.size != 2) return time24
        var hour = parts[0].toIntOrNull() ?: 8
        val minute = parts[1].toIntOrNull() ?: 0
        if (targetAm && hour >= 12) {
            hour -= 12
        } else if (!targetAm && hour < 12) {
            hour += 12
        }
        return String.format("%02d:%02d", hour, minute)
    }
}
