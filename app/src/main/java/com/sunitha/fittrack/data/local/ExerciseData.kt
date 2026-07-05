package com.sunitha.fittrack.data.local

data class ExerciseTemplate(
    val name: String,
    val muscleGroup: String,
    val defaultSets: Int = 3,
    val defaultReps: String = "8-12",
    val suggestedWeight: String = "Ask AI"
)

object ExerciseData {

    val muscleGroups = listOf(
        "Chest & Triceps",
        "Back & Biceps",
        "Legs",
        "Glutes",
        "Shoulders",
        "Full Body",
        "Abs",
        "Cardio"
    )

    val exercises: Map<String, List<ExerciseTemplate>> = mapOf(

        "Chest & Triceps" to listOf(
            ExerciseTemplate("Barbell Bench Press",       "Chest & Triceps", 4, "6-8",   "60-80 kg"),
            ExerciseTemplate("Dumbbell Bench Press",      "Chest & Triceps", 3, "10-12", "20-30 kg/hand"),
            ExerciseTemplate("Incline Bench Press",       "Chest & Triceps", 3, "8-10",  "50-70 kg"),
            ExerciseTemplate("Decline Bench Press",       "Chest & Triceps", 3, "8-10",  "55-75 kg"),
            ExerciseTemplate("Cable Fly",                 "Chest & Triceps", 3, "12-15", "10-15 kg/side"),
            ExerciseTemplate("Pec Deck Machine",          "Chest & Triceps", 3, "12-15", "40-60 kg"),
            ExerciseTemplate("Push-Ups",                  "Chest & Triceps", 3, "15-20", "Bodyweight"),
            ExerciseTemplate("Dips (Chest Focus)",        "Chest & Triceps", 3, "10-12", "Bodyweight"),
            ExerciseTemplate("Tricep Pushdown",           "Chest & Triceps", 3, "12-15", "20-30 kg"),
            ExerciseTemplate("Skull Crushers",            "Chest & Triceps", 3, "10-12", "20-30 kg"),
            ExerciseTemplate("Overhead Tricep Extension", "Chest & Triceps", 3, "12-15", "15-20 kg"),
            ExerciseTemplate("Close Grip Bench Press",    "Chest & Triceps", 3, "8-10",  "50-65 kg")
        ),

        "Back & Biceps" to listOf(
            ExerciseTemplate("Deadlift",                  "Back & Biceps", 4, "4-6",   "80-120 kg"),
            ExerciseTemplate("Pull-Ups",                  "Back & Biceps", 3, "8-12",  "Bodyweight"),
            ExerciseTemplate("Lat Pulldown",              "Back & Biceps", 3, "10-12", "50-70 kg"),
            ExerciseTemplate("Seated Cable Row",          "Back & Biceps", 3, "10-12", "50-65 kg"),
            ExerciseTemplate("Bent-Over Barbell Row",     "Back & Biceps", 3, "8-10",  "60-80 kg"),
            ExerciseTemplate("T-Bar Row",                 "Back & Biceps", 3, "10-12", "40-60 kg"),
            ExerciseTemplate("Single-Arm Dumbbell Row",  "Back & Biceps", 3, "10-12", "20-30 kg"),
            ExerciseTemplate("Face Pull",                 "Back & Biceps", 3, "15-20", "15-20 kg"),
            ExerciseTemplate("Barbell Curl",              "Back & Biceps", 3, "10-12", "20-30 kg"),
            ExerciseTemplate("Dumbbell Curl",             "Back & Biceps", 3, "10-12", "10-15 kg/hand"),
            ExerciseTemplate("Hammer Curl",               "Back & Biceps", 3, "10-12", "12-18 kg/hand"),
            ExerciseTemplate("Preacher Curl",             "Back & Biceps", 3, "10-12", "20-30 kg")
        ),

        "Legs" to listOf(
            ExerciseTemplate("Barbell Back Squat",        "Legs", 4, "6-8",    "80-120 kg"),
            ExerciseTemplate("Leg Press",                 "Legs", 3, "10-12",  "100-160 kg"),
            ExerciseTemplate("Romanian Deadlift",         "Legs", 3, "10-12",  "60-80 kg"),
            ExerciseTemplate("Lunges",                    "Legs", 3, "12/leg", "20 kg/hand"),
            ExerciseTemplate("Bulgarian Split Squat",     "Legs", 3, "10/leg", "15-25 kg/hand"),
            ExerciseTemplate("Leg Extension",             "Legs", 3, "12-15",  "40-60 kg"),
            ExerciseTemplate("Leg Curl (Lying)",          "Legs", 3, "12-15",  "30-50 kg"),
            ExerciseTemplate("Calf Raises",               "Legs", 4, "15-20",  "60-80 kg"),
            ExerciseTemplate("Goblet Squat",              "Legs", 3, "12-15",  "20-30 kg"),
            ExerciseTemplate("Hack Squat",                "Legs", 3, "10-12",  "60-100 kg")
        ),

        "Glutes" to listOf(
            ExerciseTemplate("Barbell Hip Thrust",        "Glutes", 4, "8-12",   "60-100 kg"),
            ExerciseTemplate("Glute Bridge",               "Glutes", 3, "12-15",  "20-40 kg"),
            ExerciseTemplate("Cable Kickback",             "Glutes", 3, "12-15/side", "10-20 kg"),
            ExerciseTemplate("Sumo Squat",                 "Glutes", 3, "10-12",  "30-50 kg"),
            ExerciseTemplate("Step-Ups",                   "Glutes", 3, "10-12/leg", "10-20 kg/hand"),
            ExerciseTemplate("Hip Abduction Machine",      "Glutes", 3, "15-20",  "30-50 kg"),
            ExerciseTemplate("Donkey Kicks",                "Glutes", 3, "15-20/side", "Bodyweight"),
            ExerciseTemplate("Frog Pumps",                  "Glutes", 3, "15-20",  "Bodyweight")
        ),

        "Shoulders" to listOf(
            ExerciseTemplate("Overhead Press (Barbell)",  "Shoulders", 4, "6-8",   "50-70 kg"),
            ExerciseTemplate("Dumbbell Shoulder Press",   "Shoulders", 3, "10-12", "15-25 kg/hand"),
            ExerciseTemplate("Lateral Raise",             "Shoulders", 3, "12-15", "8-12 kg/hand"),
            ExerciseTemplate("Front Raise",               "Shoulders", 3, "12-15", "8-12 kg/hand"),
            ExerciseTemplate("Rear Delt Fly",             "Shoulders", 3, "15-20", "5-10 kg/hand"),
            ExerciseTemplate("Arnold Press",              "Shoulders", 3, "10-12", "15-20 kg/hand"),
            ExerciseTemplate("Shrugs",                    "Shoulders", 3, "12-15", "30-50 kg/hand"),
            ExerciseTemplate("Upright Row",               "Shoulders", 3, "10-12", "30-40 kg")
        ),

        "Full Body" to listOf(
            ExerciseTemplate("Burpees",                   "Full Body", 3, "15-20", "Bodyweight"),
            ExerciseTemplate("Clean & Press",             "Full Body", 3, "6-8",   "40-60 kg"),
            ExerciseTemplate("Kettlebell Swing",          "Full Body", 3, "15-20", "16-24 kg"),
            ExerciseTemplate("Box Jump",                  "Full Body", 3, "10",    "Bodyweight"),
            ExerciseTemplate("Farmer's Walk",             "Full Body", 3, "30s",   "20-30 kg/hand"),
            ExerciseTemplate("Thruster",                  "Full Body", 3, "10-12", "30-50 kg"),
            ExerciseTemplate("Battle Rope",               "Full Body", 3, "30s",   "Bodyweight")
        ),

        "Abs" to listOf(
            ExerciseTemplate("Plank",                     "Abs", 3, "60s",    "Bodyweight"),
            ExerciseTemplate("Crunches",                  "Abs", 3, "20-25",  "Bodyweight"),
            ExerciseTemplate("Russian Twist",             "Abs", 3, "20/side","5-10 kg"),
            ExerciseTemplate("Leg Raises",                "Abs", 3, "15-20",  "Bodyweight"),
            ExerciseTemplate("Cable Crunch",              "Abs", 3, "15-20",  "20-30 kg"),
            ExerciseTemplate("Dead Bug",                  "Abs", 3, "10/side","Bodyweight"),
            ExerciseTemplate("Ab Rollout",                "Abs", 3, "10-12",  "Bodyweight"),
            ExerciseTemplate("Hanging Knee Raises",       "Abs", 3, "15-20",  "Bodyweight")
        ),

        "Cardio" to listOf(
            ExerciseTemplate("Treadmill Run",  "Cardio", 1, "20-30 min", "Body"),
            ExerciseTemplate("Cycling",        "Cardio", 1, "20-30 min", "Body"),
            ExerciseTemplate("Elliptical",     "Cardio", 1, "20-30 min", "Body"),
            ExerciseTemplate("Rowing Machine", "Cardio", 1, "15-30 min", "Body"),
            ExerciseTemplate("Air Bike",       "Cardio", 1, "15-30 min", "Body"),
            ExerciseTemplate("Stair Climber",  "Cardio", 1, "15-30 min", "Body"),
            ExerciseTemplate("Jump Rope",      "Cardio", 1, "5-10 min",  "Bodyweight"),
            ExerciseTemplate("HIIT Sprints",   "Cardio", 1, "20-25 min", "Bodyweight")
        )
    )

    fun forMuscleGroup(group: String): List<ExerciseTemplate> =
        exercises[group] ?: emptyList()
}
