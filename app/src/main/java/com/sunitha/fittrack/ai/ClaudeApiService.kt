package com.sunitha.fittrack.ai

import com.sunitha.fittrack.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Claude API key is read from BuildConfig, which is generated from the
 * CLAUDE_API_KEY entry in local.properties (gitignored, never committed).
 */
object ClaudeApiService {

    private val CLAUDE_API_KEY       = BuildConfig.CLAUDE_API_KEY
    private const val API_URL        = "https://api.anthropic.com/v1/messages"
    private const val MODEL          = "claude-sonnet-4-6"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // ── Exercise suggestions ──────────────────────────────────────────────────

    data class AiExercise(
        val name: String,
        val sets: Int,
        val reps: String,
        val weight: String,
        val tip: String
    )

    suspend fun getExerciseSuggestions(muscleGroup: String): Result<List<AiExercise>> =
        withContext(Dispatchers.IO) {
            val prompt = """
                You are an expert personal trainer. Suggest 6 exercises for $muscleGroup.
                Respond ONLY with a valid JSON array (no markdown, no extra text):
                [{"name":"...","sets":3,"reps":"10-12","weight":"20 kg","tip":"..."}]
                Keep tips to one sentence. Include both compound and isolation exercises.
            """.trimIndent()

            callClaude(prompt).mapCatching { text ->
                val arr = JSONArray(text.trim())
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    AiExercise(
                        name   = obj.getString("name"),
                        sets   = obj.getInt("sets"),
                        reps   = obj.getString("reps"),
                        weight = obj.getString("weight"),
                        tip    = obj.optString("tip", "")
                    )
                }
            }
        }

    // ── Food macro lookup ─────────────────────────────────────────────────────

    data class AiFoodResult(
        val name: String,
        val serving: String,
        val calories: Int,
        val proteinG: Float,
        val carbsG: Float,
        val fatG: Float,
        val fiberG: Float = 0f,
        val category: String
    )

    suspend fun getFoodMacros(foodName: String): Result<AiFoodResult> =
        withContext(Dispatchers.IO) {
            val prompt = """
                You are a nutrition expert. Provide accurate macros for: "$foodName"
                Respond ONLY with valid JSON (no markdown, no extra text):
                {"name":"exact food name","serving":"serving size","calories":200,"proteinG":5.0,"carbsG":30.0,"fatG":8.0,"fiberG":2.0,"category":"category"}
                Use Indian measurements: 1 katori=150ml, 1 cup=240ml, 1 piece for items like idli/dosa.
                Category must be one of: Grains, Dal & Legumes, South Indian, Dairy, Fats & Oils, Eggs, Non-Veg, Vegetables, Bread & Cereals, International, Beverages, Nuts & Seeds, Fruits, Junk Food, Biscuits, Sweets, Protein, Other.
            """.trimIndent()

            callClaude(prompt).mapCatching { text ->
                val clean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val obj = JSONObject(clean)
                AiFoodResult(
                    name     = obj.getString("name"),
                    serving  = obj.getString("serving"),
                    calories = obj.getInt("calories"),
                    proteinG = obj.getDouble("proteinG").toFloat(),
                    carbsG   = obj.getDouble("carbsG").toFloat(),
                    fatG     = obj.getDouble("fatG").toFloat(),
                    fiberG   = obj.optDouble("fiberG", 0.0).toFloat(),
                    category = obj.optString("category", "Other")
                )
            }
        }

    // ── Weekly AI insights ────────────────────────────────────────────────────

    // Shared anti-hallucination clause — every prompt that reasons over the user's
    // logged data includes this verbatim so the model is never allowed to fill gaps
    // with plausible-sounding but invented numbers, dates, or exercise names.
    private const val GROUNDING_RULE =
        "Ground every number, date, and name in the data given below — never estimate, round speculatively, or invent one. " +
        "If the data needed for a claim isn't present, omit the claim instead of guessing."

    suspend fun getWeeklyInsights(
        userName: String,
        workoutSection: String,
        macroSection: String,
        weightSection: String,
        periodSection: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val system = """
            You are ${userName.ifBlank { "the user" }}'s personal fitness coach. Write a concise, personalised weekly review using ONLY the data below. $GROUNDING_RULE

            FORMAT:
            - **Bold** subheadings, 2-3 crisp sentences per section, cite the exact numbers/dates from the data
            - Only include a section if relevant data is present for it
            - Tone: direct, supportive, coach-like

            SECTIONS (only when data exists):
            **💪 Workout & Progressive Overload** — per exercise with 2+ sessions: up/same/down, name + kg change
            **🥗 Nutrition** — actuals vs goals per day; best day and any deficit/surplus days by date; flag sustained low protein/calories
            **⚖️ Weight Trend** — direction, total change, pace
            **🩸 Cycle & Recovery** — only if period days tracked: days logged vs typical cycle length; nutrition adequacy (iron, complex carbs, calories) during those days; one specific action for next cycle
        """.trimIndent()

        val userData = buildString {
            if (workoutSection.isNotBlank()) { append(workoutSection); append("\n\n") }
            append(macroSection)
            if (weightSection.isNotBlank()) { append("\n\n"); append(weightSection) }
            if (periodSection.isNotBlank()) { append("\n\n"); append(periodSection) }
        }

        callClaudeWithSystem(system, userData, maxTokens = 1200)
            .map { content -> appendGroundingWarning(content, userData) }
    }

    // ── Rest day recommendation ───────────────────────────────────────────────

    suspend fun getRestDayAdvice(
        recentWorkouts: String,
        daysSinceRest: Int,
        symptoms: String,
        userContext: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val profileLine = if (userContext.isNotBlank()) "$userContext\n\n" else ""
        val data = "Recent workouts: $recentWorkouts | Days since last full rest: $daysSinceRest | Symptoms: ${symptoms.ifBlank { "none" }}"
        val prompt = """
            ${profileLine}You are a fitness recovery specialist. $GROUNDING_RULE

            $data

            Recommend ONE of: Full Rest | Active Recovery | Train (specify muscle group).
            Give 2-3 sentences explaining why, referencing the data above. Factor in any hormonal notes from the profile.
            Format: RECOMMENDATION: [option]\nREASON: [explanation]
        """.trimIndent()
        callClaude(prompt).map { content -> appendGroundingWarning(content, "$profileLine $data") }
    }

    // ── Action chips for insights ─────────────────────────────────────────────

    suspend fun getActionChips(insightContent: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            val prompt = """
                Based on this fitness insight:
                $insightContent

                Suggest exactly 3 short follow-up questions the user might want to explore (max 6 words each).
                Respond ONLY with a JSON array of 3 strings. No markdown, no extra text.
                Example: ["How to increase protein?","Best recovery foods?","Improve sleep quality?"]
            """.trimIndent()
            callClaude(prompt).mapCatching { text ->
                val clean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val arr = JSONArray(clean)
                (0 until arr.length()).map { arr.getString(it) }.take(3)
            }
        }

    suspend fun answerFollowUp(
        question: String,
        insightContext: String,
        userContext: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val profileLine = if (userContext.isNotBlank()) "User profile: $userContext\n\n" else ""
        val prompt = """
            ${profileLine}Context — recent fitness analysis:
            $insightContext

            User question: $question

            Answer in 2-3 sentences, specific and practical for an Indian diet/fitness context. $GROUNDING_RULE
        """.trimIndent()
        callClaude(prompt)
    }

    // ── Exercise cache serialization ──────────────────────────────────────────

    fun serializeExercises(exercises: List<AiExercise>): String {
        val arr = JSONArray()
        exercises.forEach { ex ->
            arr.put(JSONObject().apply {
                put("name",   ex.name)
                put("sets",   ex.sets)
                put("reps",   ex.reps)
                put("weight", ex.weight)
                put("tip",    ex.tip)
            })
        }
        return arr.toString()
    }

    fun deserializeExercises(json: String): List<AiExercise> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            AiExercise(
                name   = obj.getString("name"),
                sets   = obj.getInt("sets"),
                reps   = obj.getString("reps"),
                weight = obj.getString("weight"),
                tip    = obj.optString("tip", "")
            )
        }
    }

    // ── Hallucination guard ───────────────────────────────────────────────────
    // Dates are the strongest low-false-positive signal for invented data: unlike
    // percentages or averages (which are legitimately derived from the source),
    // the model has no reason to ever produce a calendar date that isn't in its
    // input. If one shows up anyway, flag it rather than let it pass silently.
    private val datePattern = Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2}")

    private fun appendGroundingWarning(response: String, sourceData: String): String {
        val sourceDates   = datePattern.findAll(sourceData).map { it.value }.toSet()
        val responseDates = datePattern.findAll(response).map { it.value }.toSet()
        val ungrounded    = responseDates - sourceDates
        return if (ungrounded.isEmpty()) response
        else "$response\n\n⚠️ _Could not verify against your logs: ${ungrounded.joinToString(", ")}. Double-check before relying on this._"
    }

    // ── Core HTTP call ────────────────────────────────────────────────────────

    private fun callClaudeWithSystem(system: String, userMessage: String, maxTokens: Int = 1024): Result<String> = runCatching {
        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", maxTokens)
            put("system", system)
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                }
            ))
        }.toString()

        val request = Request.Builder()
            .url(API_URL)
            .header("x-api-key", CLAUDE_API_KEY)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Claude API error ${response.code}: ${response.body?.string()}")
            }
            val json = JSONObject(response.body!!.string())
            json.getJSONArray("content").getJSONObject(0).getString("text")
        }
    }

    private fun callClaude(userMessage: String): Result<String> = runCatching {
        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 1024)
            put("messages", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                }
            ))
        }.toString()

        val request = Request.Builder()
            .url(API_URL)
            .header("x-api-key", CLAUDE_API_KEY)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Claude API error ${response.code}: ${response.body?.string()}")
            }
            val json = JSONObject(response.body!!.string())
            json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
        }
    }
}
