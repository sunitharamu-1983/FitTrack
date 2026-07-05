package com.sunitha.fittrack.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

enum class Gender        { FEMALE, MALE, PREFER_NOT_TO_SAY }
enum class FitnessGoal  { CUT, MAINTAIN, BULK }
enum class ActivityLevel { SEDENTARY, MODERATE, ACTIVE }

data class UserProfile(
    val name:          String        = "",
    val dobMillis:     Long          = 0L,
    val gender:        Gender        = Gender.PREFER_NOT_TO_SAY,
    val heightCm:      Float         = 0f,
    val goal:          FitnessGoal   = FitnessGoal.MAINTAIN,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val isSetUp:       Boolean       = false
)

data class MacroGoals(
    val calories:  Int   = 2000,
    val proteinG:  Float = 70f,
    val carbsG:    Float = 220f,
    val fatG:      Float = 65f,
    val fiberG:    Float = 25f,
    val stepGoal:  Int   = 8000
)

fun UserProfile.ageYears(): Int? {
    if (dobMillis <= 0L) return null
    val dob = Calendar.getInstance().apply { timeInMillis = dobMillis }
    return Calendar.getInstance().get(Calendar.YEAR) - dob.get(Calendar.YEAR)
}

fun UserProfile.toAiContext(weightKg: Float?, isPeriodDay: Boolean = false): String {
    if (!isSetUp) return ""
    val age       = ageYears()
    val genderStr = when (gender) {
        Gender.FEMALE            -> "female"
        Gender.MALE              -> "male"
        Gender.PREFER_NOT_TO_SAY -> "person"
    }
    val goalStr = when (goal) {
        FitnessGoal.CUT      -> "lose weight"
        FitnessGoal.MAINTAIN -> "maintain weight"
        FitnessGoal.BULK     -> "build muscle"
    }
    val activityStr = when (activityLevel) {
        ActivityLevel.SEDENTARY -> "sedentary"
        ActivityLevel.MODERATE  -> "moderately active"
        ActivityLevel.ACTIVE    -> "very active"
    }
    val hormoneNote = if (gender == Gender.FEMALE && age != null) when {
        age >= 45 -> " Consider perimenopause factors: hormonal fluctuations, sleep disruption, increased bone density needs, and muscle mass preservation."
        age in 18..44 -> " Consider menstrual cycle phases and their effect on energy levels, strength, and recovery when relevant."
        else -> ""
    } else ""

    return buildString {
        append("User profile: ")
        if (name.isNotBlank()) append("${name.trim()}, ")
        if (age != null) append("age $age, ")
        append(genderStr)
        if (weightKg != null && weightKg > 0f) append(", ${weightKg}kg")
        if (heightCm > 0f) append(", ${heightCm.toInt()}cm")
        append(", $activityStr, goal: $goalStr.")
        append(hormoneNote)
        if (isPeriodDay && gender == Gender.FEMALE) {
            append(" Note: user is currently on their period — consider lower energy levels, potential cramping, increased iron needs, and lighter workout intensity where appropriate.")
        }
    }
}

fun calculateMacros(profile: UserProfile, weightKg: Float): MacroGoals {
    if (!profile.isSetUp || weightKg <= 0f || profile.heightCm <= 0f) return MacroGoals()

    val age = (profile.ageYears() ?: 30).coerceAtLeast(15)

    val bmrBase = (10f * weightKg) + (6.25f * profile.heightCm) - (5f * age)
    val bmr = when (profile.gender) {
        Gender.FEMALE            -> bmrBase - 161f
        Gender.MALE              -> bmrBase + 5f
        Gender.PREFER_NOT_TO_SAY -> bmrBase - 78f   // midpoint
    }

    val tdee = bmr * when (profile.activityLevel) {
        ActivityLevel.SEDENTARY -> 1.2f
        ActivityLevel.MODERATE  -> 1.55f
        ActivityLevel.ACTIVE    -> 1.725f
    }

    val totalCalories = (tdee + when (profile.goal) {
        FitnessGoal.CUT      -> -300f
        FitnessGoal.MAINTAIN -> 0f
        FitnessGoal.BULK     -> 300f
    }).toInt().coerceAtLeast(1200)

    val proteinG    = weightKg                          // ICMR: 1g per kg body weight
    val proteinCals = proteinG * 4f
    val remaining   = (totalCalories - proteinCals).coerceAtLeast(0f)
    val carbsG      = remaining * 0.60f / 4f
    val fatG        = remaining * 0.40f / 9f

    val stepGoal = when (profile.activityLevel) {
        ActivityLevel.SEDENTARY -> 6000
        ActivityLevel.MODERATE  -> 8000
        ActivityLevel.ACTIVE    -> 10_000
    }

    val fiberG = when (profile.gender) {
        Gender.MALE -> 30f
        else        -> 25f
    }

    return MacroGoals(
        calories = totalCalories,
        proteinG = proteinG,
        carbsG   = carbsG,
        fatG     = fatG,
        fiberG   = fiberG,
        stepGoal = stepGoal
    )
}

private val Context.profileDataStore by preferencesDataStore(name = "user_profile")

class UserProfileStore(private val context: Context) {

    private object Keys {
        val NAME      = stringPreferencesKey("name")
        val DOB       = longPreferencesKey("dob_millis")
        val GENDER    = stringPreferencesKey("gender")
        val HEIGHT    = floatPreferencesKey("height_cm")
        val GOAL      = stringPreferencesKey("goal")
        val ACTIVITY  = stringPreferencesKey("activity_level")
        val IS_SET_UP = booleanPreferencesKey("is_set_up")
    }

    val profile: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        UserProfile(
            name          = prefs[Keys.NAME] ?: "",
            dobMillis     = prefs[Keys.DOB] ?: 0L,
            gender        = runCatching { Gender.valueOf(prefs[Keys.GENDER] ?: "") }.getOrDefault(Gender.PREFER_NOT_TO_SAY),
            heightCm      = prefs[Keys.HEIGHT] ?: 0f,
            goal          = runCatching { FitnessGoal.valueOf(prefs[Keys.GOAL] ?: "") }.getOrDefault(FitnessGoal.MAINTAIN),
            activityLevel = runCatching { ActivityLevel.valueOf(prefs[Keys.ACTIVITY] ?: "") }.getOrDefault(ActivityLevel.MODERATE),
            isSetUp       = prefs[Keys.IS_SET_UP] ?: false
        )
    }

    suspend fun save(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.NAME]      = profile.name
            prefs[Keys.DOB]       = profile.dobMillis
            prefs[Keys.GENDER]    = profile.gender.name
            prefs[Keys.HEIGHT]    = profile.heightCm
            prefs[Keys.GOAL]      = profile.goal.name
            prefs[Keys.ACTIVITY]  = profile.activityLevel.name
            prefs[Keys.IS_SET_UP] = true
        }
    }
}
