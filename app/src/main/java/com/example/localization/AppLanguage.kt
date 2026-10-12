package com.example.localization

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी")
}

object AppStrings {
    // Helper to get bilingual string based on current language
    fun get(lang: AppLanguage, en: String, hi: String): String {
        return if (lang == AppLanguage.HINDI) hi else en
    }
}
