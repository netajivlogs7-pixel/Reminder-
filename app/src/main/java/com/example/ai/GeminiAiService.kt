package com.example.ai

import com.example.BuildConfig
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityEntity
import com.example.data.model.RepeatType
import com.example.data.model.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ParsedTaskProposal(
    val title: String,
    val description: String,
    val category: ActivityCategory,
    val startTime: String, // HH:mm
    val durationMinutes: Int,
    val repeatType: RepeatType,
    val priority: TaskPriority
) {
    fun toEntity(): ActivityEntity {
        val color = when (category) {
            ActivityCategory.WORKOUT -> 0xFFEA580C
            ActivityCategory.HYDRATION -> 0xFF0284C7
            ActivityCategory.STUDY -> 0xFF4F46E5
            ActivityCategory.SLEEP -> 0xFF8B5CF6
            ActivityCategory.MEDICATION -> 0xFFEF4444
            ActivityCategory.ROUTINE -> 0xFF0D9488
            ActivityCategory.CUSTOM -> 0xFF6366F1
        }
        return ActivityEntity(
            title = title,
            description = description,
            category = category.name,
            startTime = startTime,
            durationMinutes = durationMinutes,
            repeatType = repeatType.name,
            priority = priority.name,
            colorHex = color
        )
    }
}

data class AiAssistantResponse(
    val replyText: String,
    val proposedTask: ParsedTaskProposal? = null
)

class GeminiAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val modelName = "gemini-3.5-flash"
    private val apiKey = BuildConfig.GEMINI_API_KEY

    suspend fun processUserMessage(
        userMessage: String,
        currentLanguage: String = "en",
        contextSummary: String = ""
    ): AiAssistantResponse = withContext(Dispatchers.IO) {
        val trimmed = userMessage.trim()
        if (trimmed.isEmpty()) {
            return@withContext AiAssistantResponse(
                replyText = if (currentLanguage == "hi") "कृपया कोई संदेश या कार्य बताएं।" else "Please enter a message or routine task."
            )
        }

        // Check if API key is configured
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent rule-based NLP fallback in Hindi & English
            return@withContext fallbackLocalParser(trimmed, currentLanguage)
        }

        try {
            val systemInstruction = """
                You are the AI Assistant for 'Daily Life Manager' mobile app.
                Support both Hindi and English fluently.
                When the user asks to schedule, remind, or create a task/routine (e.g. 'Kal subah 6 baje running', 'Schedule study at 7pm'):
                You MUST return a JSON response with this structure:
                {
                  "reply": "friendly explanation in the user's language",
                  "proposal": {
                    "title": "Clean concise task name",
                    "description": "Optional notes or details",
                    "category": "ROUTINE" | "WORKOUT" | "HYDRATION" | "STUDY" | "SLEEP" | "MEDICATION" | "CUSTOM",
                    "startTime": "HH:mm in 24hr format",
                    "durationMinutes": 30,
                    "repeatType": "DAILY" | "ONCE" | "WEEKDAYS" | "WEEKLY",
                    "priority": "LOW" | "MEDIUM" | "HIGH"
                  }
                }
                If the user asks for a routine recommendation or daily summary, provide an inspiring, actionable reply and optional proposed task.
                Respond with valid raw JSON only.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", "Context: $contextSummary\nUser: $trimmed") })
                        }
                        put("parts", parts)
                    })
                }
                put("contents", contents)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                return@withContext fallbackLocalParser(trimmed, currentLanguage)
            }

            val respObj = JSONObject(responseBody)
            val candidates = respObj.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (text.isNullOrBlank()) {
                return@withContext fallbackLocalParser(trimmed, currentLanguage)
            }

            val parsedJson = JSONObject(text.trim())
            val reply = parsedJson.optString("reply", "Here is what I prepared for you.")
            var proposal: ParsedTaskProposal? = null

            if (parsedJson.has("proposal") && !parsedJson.isNull("proposal")) {
                val p = parsedJson.getJSONObject("proposal")
                proposal = ParsedTaskProposal(
                    title = p.optString("title", "Daily Task"),
                    description = p.optString("description", ""),
                    category = parseCategory(p.optString("category", "ROUTINE")),
                    startTime = validateTime(p.optString("startTime", "08:00")),
                    durationMinutes = p.optInt("durationMinutes", 30),
                    repeatType = parseRepeat(p.optString("repeatType", "DAILY")),
                    priority = parsePriority(p.optString("priority", "MEDIUM"))
                )
            }

            AiAssistantResponse(replyText = reply, proposedTask = proposal)

        } catch (e: Exception) {
            fallbackLocalParser(trimmed, currentLanguage)
        }
    }

    private fun fallbackLocalParser(input: String, lang: String): AiAssistantResponse {
        val lower = input.lowercase()
        var category = ActivityCategory.ROUTINE
        var time = "08:00"
        var duration = 30
        var priority = TaskPriority.MEDIUM
        var repeat = RepeatType.DAILY

        // Detect Category
        if (lower.contains("water") || lower.contains("paani") || lower.contains("पानी") || lower.contains("hydration")) {
            category = ActivityCategory.HYDRATION
            duration = 5
        } else if (lower.contains("run") || lower.contains("walk") || lower.contains("gym") || lower.contains("workout") || lower.contains("दौड़") || lower.contains("व्यायाम") || lower.contains("सैर")) {
            category = ActivityCategory.WORKOUT
            duration = 45
            priority = TaskPriority.HIGH
        } else if (lower.contains("study") || lower.contains("padh") || lower.contains("पढ़") || lower.contains("exam") || lower.contains("book") || lower.contains("work")) {
            category = ActivityCategory.STUDY
            duration = 60
            priority = TaskPriority.HIGH
        } else if (lower.contains("sleep") || lower.contains("so") || lower.contains("सोने") || lower.contains("bedtime")) {
            category = ActivityCategory.SLEEP
            duration = 30
            time = "22:00"
        } else if (lower.contains("med") || lower.contains("dawa") || lower.contains("दवा") || lower.contains("pill")) {
            category = ActivityCategory.MEDICATION
            duration = 5
            priority = TaskPriority.HIGH
        }

        // Detect Time (e.g. 6 baje, 6 am, 7:30 pm, 8:00)
        val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|baje|बजे)?", RegexOption.IGNORE_CASE)
        val match = timeRegex.find(lower)
        if (match != null) {
            var hour = match.groupValues[1].toIntOrNull() ?: 8
            val minute = match.groupValues[2].ifEmpty { "00" }
            val modifier = match.groupValues[3].lowercase()

            if (modifier == "pm" && hour < 12) {
                hour += 12
            } else if (modifier == "am" && hour == 12) {
                hour = 0
            } else if ((modifier == "baje" || modifier == "बजे") && lower.contains("shaam") || lower.contains("शाम") || lower.contains("evening") || lower.contains("night") || lower.contains("raat")) {
                if (hour < 12) hour += 12
            }
            time = String.format("%02d:%s", hour, minute)
        }

        // Clean title
        val title = input.replace(Regex("(?i)(kal|aaj|reminder|lagao|karo|schedule|please|subah|shaam|baje|बजे)"), "").trim().ifBlank {
            if (lang == "hi") "नया दैनिक कार्य" else "New Daily Activity"
        }

        val proposal = ParsedTaskProposal(
            title = title.replaceFirstChar { it.uppercase() },
            description = if (lang == "hi") "एआई द्वारा नियोजित कार्य" else "Scheduled via AI Assistant",
            category = category,
            startTime = time,
            durationMinutes = duration,
            repeatType = repeat,
            priority = priority
        )

        val reply = if (lang == "hi") {
            "मैंने आपके लिए '${proposal.title}' का कार्य तैयार किया है (समय: $time). क्या आप इसे जोड़ना चाहते हैं?"
        } else {
            "I've structured a task for '${proposal.title}' at $time. Review and confirm below to add it to your schedule."
        }

        return AiAssistantResponse(replyText = reply, proposedTask = proposal)
    }

    private fun parseCategory(cat: String): ActivityCategory {
        return try {
            ActivityCategory.valueOf(cat.uppercase())
        } catch (e: Exception) {
            ActivityCategory.ROUTINE
        }
    }

    private fun parseRepeat(rep: String): RepeatType {
        return try {
            RepeatType.valueOf(rep.uppercase())
        } catch (e: Exception) {
            RepeatType.DAILY
        }
    }

    private fun parsePriority(p: String): TaskPriority {
        return try {
            TaskPriority.valueOf(p.uppercase())
        } catch (e: Exception) {
            TaskPriority.MEDIUM
        }
    }

    private fun validateTime(t: String): String {
        return if (t.matches(Regex("\\d{2}:\\d{2}"))) t else "08:00"
    }
}
