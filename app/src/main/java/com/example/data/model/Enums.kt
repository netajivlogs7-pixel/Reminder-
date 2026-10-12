package com.example.data.model

enum class ActivityCategory(val displayNameEn: String, val displayNameHi: String) {
    ROUTINE("Daily Routine", "दैनिक दिनचर्या"),
    WORKOUT("Exercise & Walk", "व्यायाम और वॉक"),
    HYDRATION("Water & Hydration", "जल और स्वास्थ्य"),
    STUDY("Study & Work", "पढ़ाई और कार्य"),
    SLEEP("Sleep Routine", "नींद की दिनचर्या"),
    MEDICATION("Medicine & Care", "दवा और स्वास्थ्य"),
    CUSTOM("Custom Task", "कस्टम कार्य")
}

enum class RepeatType(val displayNameEn: String, val displayNameHi: String) {
    ONCE("One Time", "एक बार"),
    DAILY("Every Day", "प्रतिदिन"),
    WEEKDAYS("Mon - Fri", "सोमवार - शुक्रवार"),
    WEEKENDS("Sat - Sun", "शनिवार - रविवार"),
    WEEKLY("Weekly", "साप्ताहिक"),
    MONTHLY("Monthly", "मासिक")
}

enum class TaskPriority(val displayNameEn: String, val displayNameHi: String, val colorHex: Long) {
    LOW("Low", "कम", 0xFF10B981),
    MEDIUM("Medium", "मध्यम", 0xFFF59E0B),
    HIGH("High", "उच्च", 0xFFEF4444)
}

enum class SubscriptionTier {
    FREE,
    PRO
}
