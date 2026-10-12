package com.example.speech

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult

object SpeechHelper {

    fun createSpeechIntent(languageCode: String): Intent {
        val locale = if (languageCode == "hi") "hi-IN" else "en-US"
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (languageCode == "hi") "अपनी दिनचर्या या कार्य बोलें..." else "Speak your routine or task...")
        }
    }

    fun parseSpeechResult(result: ActivityResult): String? {
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            return spokenText
        }
        return null
    }
}
