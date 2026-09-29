package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.DayWorkoutPlan
import com.example.data.model.ExerciseItem
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && !key.equals("MY_GEMINI_API_KEY", ignoreCase = true)
    }

    suspend fun generateWorkoutPlan(profile: UserProfile): WorkoutPlan = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext getOfflineFallbackPlan(profile)
        }

        val prompt = """
You are FitBuddy AI, a certified sports scientist and Olympic strength coach.
Generate a structured 7-day workout plan and nutrition/recovery protocol for:
Name: ${profile.name}
Age: ${profile.age}, Gender: ${profile.gender}
Weight: ${profile.weightKg} kg, Height: ${profile.heightCm} cm
Fitness Level: ${profile.fitnessLevel}
Goal: ${profile.fitnessGoal}
Intensity: ${profile.intensity}
Limitations: ${profile.limitations}

Respond ONLY with valid JSON conforming to this exact structure:
{
  "title": "Inspiring Title",
  "overview": "2-3 sentences explaining the strategy",
  "days": [
    {
      "dayNumber": 1,
      "dayTitle": "Day 1: Muscle/Focus",
      "focus": "Focus Area",
      "warmUp": "Warm-up description",
      "exercises": [
        {
          "name": "Exercise Name",
          "sets": "3",
          "repsOrDuration": "10-12 reps",
          "restSeconds": "60s",
          "targetMuscle": "Target muscle",
          "formTip": "Form cue"
        }
      ],
      "coolDown": "Cool-down description",
      "isRestDay": false
    }
  ],
  "nutritionTips": [
    "Macro and calorie guidance tip 1",
    "Hydration tip 2",
    "Meal timing tip 3",
    "Target nutrition tip 4"
  ],
  "recoveryTips": [
    "Sleep and regeneration tip 1",
    "Mobility tip 2",
    "Soreness management tip 3"
  ]
}
Include exactly 7 days.
""".trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful || responseBody.isEmpty()) {
                return@withContext getOfflineFallbackPlan(profile)
            }

            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text").orEmpty().trim()

            parsePlanJson(rawText, profile)
        } catch (e: Exception) {
            getOfflineFallbackPlan(profile)
        }
    }

    suspend fun refineWorkoutPlan(currentPlan: WorkoutPlan, feedback: String): WorkoutPlan = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isApiKeyConfigured()) {
            return@withContext currentPlan.copy(
                title = "${currentPlan.title} (Refined)",
                overview = "${currentPlan.overview}\n\n[Applied Adjustment]: $feedback",
                updatedAt = "Just now",
                appliedFeedback = feedback
            )
        }

        val prompt = """
Here is an existing 7-Day Workout Plan JSON:
${workoutPlanToJson(currentPlan)}

The user provided this feedback:
"$feedback"

Update and refine the 7-day routine, exercise selections, or rest intervals to directly resolve this feedback.
Respond ONLY with updated valid JSON with keys: "title", "overview", "days", "nutritionTips", "recoveryTips".
""".trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful || responseBody.isEmpty()) {
                return@withContext currentPlan.copy(
                    overview = "${currentPlan.overview}\n\n[Adjustment]: $feedback",
                    updatedAt = "Just now",
                    appliedFeedback = feedback
                )
            }

            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            val rawText = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty()
            val profile = UserProfile(name = currentPlan.userName, fitnessGoal = currentPlan.fitnessGoal, intensity = currentPlan.intensity)
            val updated = parsePlanJson(rawText, profile)
            updated.copy(
                planId = currentPlan.planId,
                createdAt = currentPlan.createdAt,
                updatedAt = "Just now",
                appliedFeedback = feedback
            )
        } catch (e: Exception) {
            currentPlan.copy(
                overview = "${currentPlan.overview}\n\n[Adjustment]: $feedback",
                updatedAt = "Just now",
                appliedFeedback = feedback
            )
        }
    }

    private fun parsePlanJson(rawText: String, profile: UserProfile): WorkoutPlan {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) clean = clean.removePrefix("```json")
        if (clean.startsWith("```")) clean = clean.removePrefix("```")
        if (clean.endsWith("```")) clean = clean.removeSuffix("```")
        clean = clean.trim()

        val root = JSONObject(clean)
        val title = root.optString("title", "7-Day ${profile.fitnessGoal} Routine")
        val overview = root.optString("overview", "Adaptive fitness plan optimized for ${profile.name}.")

        val daysArray = root.optJSONArray("days") ?: JSONArray()
        val daysList = mutableListOf<DayWorkoutPlan>()

        for (i in 0 until daysArray.length()) {
            val dayObj = daysArray.optJSONObject(i) ?: continue
            val exercisesArray = dayObj.optJSONArray("exercises") ?: JSONArray()
            val exercisesList = mutableListOf<ExerciseItem>()

            for (j in 0 until exercisesArray.length()) {
                val exObj = exercisesArray.optJSONObject(j) ?: continue
                exercisesList.add(
                    ExerciseItem(
                        name = exObj.optString("name", "Exercise"),
                        sets = exObj.optString("sets", "3"),
                        repsOrDuration = exObj.optString("repsOrDuration", "10-12 reps"),
                        restSeconds = exObj.optString("restSeconds", "60s"),
                        targetMuscle = exObj.optString("targetMuscle", "Core"),
                        formTip = exObj.optString("formTip", "Maintain control and steady breathing")
                    )
                )
            }

            daysList.add(
                DayWorkoutPlan(
                    dayNumber = dayObj.optInt("dayNumber", i + 1),
                    dayTitle = dayObj.optString("dayTitle", "Day ${i + 1}"),
                    focus = dayObj.optString("focus", "Active Training"),
                    warmUp = dayObj.optString("warmUp", "5 mins dynamic stretches"),
                    exercises = exercisesList,
                    coolDown = dayObj.optString("coolDown", "5 mins gentle stretching"),
                    isRestDay = dayObj.optBoolean("isRestDay", exercisesList.isEmpty())
                )
            )
        }

        val nutritionTips = mutableListOf<String>()
        val nutArray = root.optJSONArray("nutritionTips")
        if (nutArray != null) {
            for (i in 0 until nutArray.length()) {
                nutritionTips.add(nutArray.optString(i))
            }
        }

        val recoveryTips = mutableListOf<String>()
        val recArray = root.optJSONArray("recoveryTips")
        if (recArray != null) {
            for (i in 0 until recArray.length()) {
                recoveryTips.add(recArray.optString(i))
            }
        }

        if (daysList.size < 7) {
            return getOfflineFallbackPlan(profile)
        }

        return WorkoutPlan(
            planId = UUID.randomUUID().toString(),
            userName = profile.name,
            fitnessGoal = profile.fitnessGoal,
            intensity = profile.intensity,
            title = title,
            overview = overview,
            days = daysList,
            nutritionTips = if (nutritionTips.isNotEmpty()) nutritionTips else listOf("Prioritize whole protein sources", "Drink 3L of water daily"),
            recoveryTips = if (recoveryTips.isNotEmpty()) recoveryTips else listOf("Aim for 8 hours of sleep", "Take post-workout stretches seriously"),
            createdAt = "Today"
        )
    }

    fun workoutPlanToJson(plan: WorkoutPlan): String {
        val root = JSONObject()
        root.put("planId", plan.planId)
        root.put("userName", plan.userName)
        root.put("fitnessGoal", plan.fitnessGoal)
        root.put("intensity", plan.intensity)
        root.put("title", plan.title)
        root.put("overview", plan.overview)
        root.put("createdAt", plan.createdAt)
        root.put("updatedAt", plan.updatedAt)
        root.put("appliedFeedback", plan.appliedFeedback)

        val daysArr = JSONArray()
        for (day in plan.days) {
            val d = JSONObject()
            d.put("dayNumber", day.dayNumber)
            d.put("dayTitle", day.dayTitle)
            d.put("focus", day.focus)
            d.put("warmUp", day.warmUp)
            d.put("coolDown", day.coolDown)
            d.put("isRestDay", day.isRestDay)
            val exArr = JSONArray()
            for (ex in day.exercises) {
                val e = JSONObject()
                e.put("name", ex.name)
                e.put("sets", ex.sets)
                e.put("repsOrDuration", ex.repsOrDuration)
                e.put("restSeconds", ex.restSeconds)
                e.put("targetMuscle", ex.targetMuscle)
                e.put("formTip", ex.formTip)
                exArr.put(e)
            }
            d.put("exercises", exArr)
            daysArr.put(d)
        }
        root.put("days", daysArr)

        val nutArr = JSONArray()
        plan.nutritionTips.forEach { nutArr.put(it) }
        root.put("nutritionTips", nutArr)

        val recArr = JSONArray()
        plan.recoveryTips.forEach { recArr.put(it) }
        root.put("recoveryTips", recArr)

        return root.toString()
    }

    fun jsonToWorkoutPlan(jsonString: String): WorkoutPlan? {
        return try {
            val root = JSONObject(jsonString)
            val daysArr = root.optJSONArray("days") ?: JSONArray()
            val daysList = mutableListOf<DayWorkoutPlan>()
            for (i in 0 until daysArr.length()) {
                val d = daysArr.getJSONObject(i)
                val exArr = d.optJSONArray("exercises") ?: JSONArray()
                val exList = mutableListOf<ExerciseItem>()
                for (j in 0 until exArr.length()) {
                    val e = exArr.getJSONObject(j)
                    exList.add(
                        ExerciseItem(
                            name = e.optString("name"),
                            sets = e.optString("sets"),
                            repsOrDuration = e.optString("repsOrDuration"),
                            restSeconds = e.optString("restSeconds"),
                            targetMuscle = e.optString("targetMuscle"),
                            formTip = e.optString("formTip")
                        )
                    )
                }
                daysList.add(
                    DayWorkoutPlan(
                        dayNumber = d.optInt("dayNumber", i + 1),
                        dayTitle = d.optString("dayTitle", "Day ${i + 1}"),
                        focus = d.optString("focus", "Workout"),
                        warmUp = d.optString("warmUp"),
                        exercises = exList,
                        coolDown = d.optString("coolDown"),
                        isRestDay = d.optBoolean("isRestDay", false)
                    )
                )
            }

            val nutList = mutableListOf<String>()
            val nutArr = root.optJSONArray("nutritionTips")
            if (nutArr != null) {
                for (i in 0 until nutArr.length()) nutList.add(nutArr.getString(i))
            }

            val recList = mutableListOf<String>()
            val recArr = root.optJSONArray("recoveryTips")
            if (recArr != null) {
                for (i in 0 until recArr.length()) recList.add(recArr.getString(i))
            }

            WorkoutPlan(
                planId = root.optString("planId", UUID.randomUUID().toString()),
                userName = root.optString("userName", "Athlete"),
                fitnessGoal = root.optString("fitnessGoal", "General Wellness"),
                intensity = root.optString("intensity", "Medium"),
                title = root.optString("title", "7-Day Workout Routine"),
                overview = root.optString("overview", "Custom FitBuddy plan"),
                days = daysList,
                nutritionTips = nutList,
                recoveryTips = recList,
                createdAt = root.optString("createdAt", "Today"),
                updatedAt = root.optString("updatedAt").takeIf { it.isNotEmpty() },
                appliedFeedback = root.optString("appliedFeedback").takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getOfflineFallbackPlan(profile: UserProfile): WorkoutPlan {
        val days = listOf(
            DayWorkoutPlan(
                dayNumber = 1, dayTitle = "Day 1: Full-Body Power & Activation", focus = "Functional Strength",
                warmUp = "5 mins arm circles, bodyweight squats, and inchworms",
                exercises = listOf(
                    ExerciseItem("Goblet Squats", "3-4", "12 reps", "60s", "Quadriceps & Glutes", "Keep chest proud, elbows tucked inside knees"),
                    ExerciseItem("Incline / Floor Push-Ups", "3", "10-12 reps", "60s", "Pectorals & Triceps", "Tight core, elbows at 45 degrees"),
                    ExerciseItem("Dumbbell / Bent-Over Rows", "3", "12 reps", "60s", "Lats & Rhomboids", "Squeeze shoulder blades firmly at peak"),
                    ExerciseItem("Plank Hold", "3", "45 seconds", "45s", "Anterior Core", "Straight line from heels to crown of head")
                ),
                coolDown = "5 mins child's pose and chest doorway stretch"
            ),
            DayWorkoutPlan(
                dayNumber = 2, dayTitle = "Day 2: Cardio & Core Engine", focus = "Aerobic Endurance",
                warmUp = "3 mins light marching or jump rope simulation",
                exercises = listOf(
                    ExerciseItem("Zone 2 Cardio (Jog/Cycle/Brisk Walk)", "1", "30-40 mins", "None", "Cardiovascular", "Maintain conversational breathing pace"),
                    ExerciseItem("Dead Bug", "3", "12 reps/side", "30s", "Deep Core", "Press lumbar spine flat to floor"),
                    ExerciseItem("Bird-Dog", "3", "12 reps/side", "30s", "Posterior Chain", "Reach through opposite heel and fingers")
                ),
                coolDown = "5 mins spinal twists and quad stretch"
            ),
            DayWorkoutPlan(
                dayNumber = 3, dayTitle = "Day 3: Lower Body Hypertrophy", focus = "Leg Strength & Power",
                warmUp = "5 mins hip openers and leg swings",
                exercises = listOf(
                    ExerciseItem("Romanian Deadlifts", "3-4", "10 reps", "75s", "Hamstrings & Glutes", "Push hips back, feel deep stretch in hamstrings"),
                    ExerciseItem("Walking Lunges", "3", "10 reps/leg", "60s", "Quads & Stabilizers", "Lower back knee softly toward floor"),
                    ExerciseItem("Glute Bridges", "3", "15 reps", "45s", "Gluteus Maximus", "2 second pause at top contraction")
                ),
                coolDown = "5 mins hamstring and hip flexor stretches"
            ),
            DayWorkoutPlan(
                dayNumber = 4, dayTitle = "Day 4: Active Recovery & Mobility Flow", focus = "Joint Restoration",
                warmUp = "Deep diaphragmatic breathing",
                exercises = listOf(
                    ExerciseItem("90/90 Hip Switches", "2", "10 reps/side", "30s", "Hip Capsule", "Smooth rotation through both hips"),
                    ExerciseItem("World's Greatest Stretch", "2", "6 reps/side", "30s", "Thoracic Spine", "Follow top hand with your gaze"),
                    ExerciseItem("Gentle Walking", "1", "20-30 mins", "None", "Full Body", "Relax shoulders, nasal breathing")
                ),
                coolDown = "10 mins foam rolling and meditation",
                isRestDay = true
            ),
            DayWorkoutPlan(
                dayNumber = 5, dayTitle = "Day 5: Upper Body Sculpt & Delts", focus = "Upper Body Push & Pull",
                warmUp = "5 mins arm swings and band pull-aparts",
                exercises = listOf(
                    ExerciseItem("Dumbbell Overhead Shoulder Press", "3", "10-12 reps", "60s", "Deltoids", "Brace core, don't hyperextend spine"),
                    ExerciseItem("Lat Pulldown or Band Pulls", "3", "12 reps", "60s", "Latissimus Dorsi", "Drive elbows down towards back pockets"),
                    ExerciseItem("Bicep Curls to Triceps Dips", "3", "12 reps each", "45s", "Arms", "Controlled tempo, no swinging")
                ),
                coolDown = "5 mins shoulder and triceps stretches"
            ),
            DayWorkoutPlan(
                dayNumber = 6, dayTitle = "Day 6: Metabolic HIIT Conditioning", focus = "Caloric Burn & Agility",
                warmUp = "5 mins dynamic movement flow",
                exercises = listOf(
                    ExerciseItem("Speed Skaters", "4", "30 seconds", "20s", "Lateral Quads & Balance", "Stay low and land soft on ball of foot"),
                    ExerciseItem("Mountain Climbers", "3", "40 seconds", "30s", "Full Body Cardio", "Drive knees smoothly toward chest"),
                    ExerciseItem("Bear Crawl Hold / Steps", "3", "30 seconds", "30s", "Shoulders & Core", "Knees hover 2 inches off floor")
                ),
                coolDown = "5 mins full-body cooldown flow"
            ),
            DayWorkoutPlan(
                dayNumber = 7, dayTitle = "Day 7: Full Rest & Regeneration", focus = "System Renewal",
                warmUp = "None",
                exercises = emptyList(),
                coolDown = "Hydration, nutritious meals, and rest",
                isRestDay = true
            )
        )

        val nutrition = listOf(
            "Consume 1.8g - 2.0g protein per kg of bodyweight daily (~${(profile.weightKg * 1.8f).toInt()}g) to fuel muscle repair.",
            "Drink at least 3 liters of water; add electrolytes during demanding training days.",
            "Time 30g of protein and complex carbohydrates within 90 minutes post-workout.",
            "Emphasize colorful whole foods: leafy greens, lean meats, eggs, oats, and berries."
        )

        val recovery = listOf(
            "Aim for 8 hours of uninterrupted sleep in a dark, quiet, cool room.",
            "Take a contrast shower (alternating cold and hot) to stimulate circulation.",
            "Practice 5 minutes of box breathing (4s in, 4s hold, 4s out, 4s hold) before bed."
        )

        return WorkoutPlan(
            planId = UUID.randomUUID().toString(),
            userName = profile.name,
            fitnessGoal = profile.fitnessGoal,
            intensity = profile.intensity,
            title = "7-Day ${profile.fitnessGoal} Protocol (${profile.intensity} Intensity)",
            overview = "Scientifically balanced weekly training split designed for ${profile.name} targeting ${profile.fitnessGoal} with ${profile.intensity} intensity.",
            days = days,
            nutritionTips = nutrition,
            recoveryTips = recovery,
            createdAt = "Today"
        )
    }
}
