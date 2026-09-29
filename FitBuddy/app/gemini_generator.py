import json
import uuid
from datetime import datetime
from typing import Dict, Any, List
from app.config import GEMINI_API_KEY
from app.schemas import FitnessProfile, GeneratedPlan, DayWorkoutPlan, ExerciseItem
from app.gemini_flash_generator import call_gemini_api

SYSTEM_INSTRUCTION = """
You are FitBuddy AI, an elite certified exercise physiologist, Olympic strength coach, and sports nutritionist.
You generate personalized, science-backed 7-day workout plans and tailored nutrition and recovery guidance.
You MUST output strictly valid JSON conforming to the requested schema. No markdown formatting, no commentary outside JSON.
"""

def get_default_fallback_plan(profile: FitnessProfile) -> GeneratedPlan:
    """Provides a high-quality deterministic 7-day plan if Gemini API is unreachable or unconfigured."""
    goal = profile.fitness_goal.lower()
    intensity = profile.intensity.lower()

    if "weight" in goal:
        plan_title = f"7-Day Caloric Burn & Lean Sculpt ({profile.intensity} Intensity)"
        overview = f"Targeted metabolic conditioning and resistance circuits tailored for {profile.user_name} to maximize caloric burn while preserving lean muscle tissue."
        days = [
            DayWorkoutPlan(
                day_number=1, day_title="Day 1: Full-Body HIIT & Core", focus="Metabolic Conditioning",
                warm_up="5 mins dynamic arm swings, high knees, and bodyweight squats",
                exercises=[
                    ExerciseItem(name="Bodyweight Squats", sets="3-4", reps_or_duration="15 reps", rest_seconds="45s", target_muscle="Quadriceps & Glutes", form_tip="Chest tall, break at hips first"),
                    ExerciseItem(name="Push-Ups (or Incline)", sets="3", reps_or_duration="10-12 reps", rest_seconds="45s", target_muscle="Chest & Triceps", form_tip="Keep core rigid in high plank"),
                    ExerciseItem(name="Mountain Climbers", sets="3", reps_or_duration="40 seconds", rest_seconds="30s", target_muscle="Full Body & Core", form_tip="Drive knees towards elbows smoothly"),
                    ExerciseItem(name="Plank Hold", sets="3", reps_or_duration="45 seconds", rest_seconds="45s", target_muscle="Transverse Abdominis", form_tip="Breathe steadily, glutes engaged")
                ],
                cool_down="5 mins hamstring and hip flexor stretches"
            ),
            DayWorkoutPlan(
                day_number=2, day_title="Day 2: Low-Impact Cardio & Core", focus="Aerobic Endurance & Recovery",
                warm_up="3 mins light walking or marching in place",
                exercises=[
                    ExerciseItem(name="Brisk Incline Walking / Cycling", sets="1", reps_or_duration="30-40 mins", rest_seconds="None", target_muscle="Cardiovascular System", form_tip="Maintain steady Zone 2 heart rate"),
                    ExerciseItem(name="Bird-Dog", sets="3", reps_or_duration="12 reps/side", rest_seconds="30s", target_muscle="Spinal Erectors & Glutes", form_tip="Avoid arching lower back"),
                    ExerciseItem(name="Dead Bug", sets="3", reps_or_duration="12 reps/side", rest_seconds="30s", target_muscle="Deep Core", form_tip="Lower back pressed flat to floor")
                ],
                cool_down="5 mins child's pose and cat-cow stretches"
            ),
            DayWorkoutPlan(
                day_number=3, day_title="Day 3: Lower Body & Glutes", focus="Hypertrophy & Caloric Spend",
                warm_up="5 mins leg swings and hip openers",
                exercises=[
                    ExerciseItem(name="Walking Lunges", sets="3", reps_or_duration="12 reps/leg", rest_seconds="60s", target_muscle="Glutes & Hamstrings", form_tip="Drop back knee gently towards floor"),
                    ExerciseItem(name="Glute Bridges", sets="3", reps_or_duration="15 reps", rest_seconds="45s", target_muscle="Gluteus Maximus", form_tip="Pause 2s at top contraction"),
                    ExerciseItem(name="Step-Ups (Chair or Box)", sets="3", reps_or_duration="12 reps/leg", rest_seconds="45s", target_muscle="Quads & Calves", form_tip="Drive through heel of lead foot")
                ],
                cool_down="5 mins quad and calf stretches"
            ),
            DayWorkoutPlan(
                day_number=4, day_title="Day 4: Active Recovery & Mobility", focus="Tissue Regeneration",
                warm_up="3 mins deep diaphragmatic breathing",
                exercises=[
                    ExerciseItem(name="Gentle Walking", sets="1", reps_or_duration="30 mins", rest_seconds="None", target_muscle="Full Body", form_tip="Relax shoulders and breathe through nose"),
                    ExerciseItem(name="Foam Rolling / Self-Massage", sets="1", reps_or_duration="15 mins", rest_seconds="None", target_muscle="Fascia & Tight Areas", form_tip="Spend 30s on tight tender spots")
                ],
                cool_down="Gentle spinal twists and seated forward fold",
                is_rest_day=True
            ),
            DayWorkoutPlan(
                day_number=5, day_title="Day 5: Upper Body & Cardio Intervals", focus="Muscular Endurance",
                warm_up="5 mins arm circles and light shadow boxing",
                exercises=[
                    ExerciseItem(name="Dumbbell/Resistance Band Rows", sets="3", reps_or_duration="12 reps", rest_seconds="60s", target_muscle="Rhomboids & Lats", form_tip="Squeeze shoulder blades together"),
                    ExerciseItem(name="Overhead Shoulder Press", sets="3", reps_or_duration="10-12 reps", rest_seconds="60s", target_muscle="Deltoids", form_tip="Brace abs to protect lower back"),
                    ExerciseItem(name="Jumping Jacks or Step Jacks", sets="4", reps_or_duration="45 seconds", rest_seconds="30s", target_muscle="Cardio", form_tip="Land softly on balls of feet")
                ],
                cool_down="5 mins chest opening doorway stretch"
            ),
            DayWorkoutPlan(
                day_number=6, day_title="Day 6: Total Body Tabata Challenge", focus="Peak Caloric Afterburn",
                warm_up="5 mins full-body dynamic flow",
                exercises=[
                    ExerciseItem(name="Speed Skaters", sets="4", reps_or_duration="30 seconds", rest_seconds="20s", target_muscle="Lateral Quads & Abductors", form_tip="Stay low and push off lateral foot"),
                    ExerciseItem(name="Inchworm to Push-Up", sets="3", reps_or_duration="8 reps", rest_seconds="45s", target_muscle="Hamstrings & Pecs", form_tip="Walk hands out slowly with control"),
                    ExerciseItem(name="Bicycle Crunches", sets="3", reps_or_duration="20 reps", rest_seconds="30s", target_muscle="Obliques", form_tip="Rotate from torso, not pulling neck")
                ],
                cool_down="5 mins cobra pose and downward dog"
            ),
            DayWorkoutPlan(
                day_number=7, day_title="Day 7: Full Rest & Recharge", focus="System Recovery",
                warm_up="Optional 10 mins morning sunlight walk",
                exercises=[],
                cool_down="Hydration and light stretching",
                is_rest_day=True
            )
        ]
        nutrition = [
            "Maintain a moderate caloric deficit of 300-500 kcal below maintenance.",
            f"Target 1.6g to 2.0g protein per kg of bodyweight (~{int(profile.weight_kg * 1.8)}g daily) to spare muscle mass.",
            "Drink at least 3 liters of water throughout the day; add electrolytes after sweaty sessions.",
            "Front-load complex carbohydrates around your workout window for sustained training energy.",
            "Emphasize high-volume, fiber-rich vegetables (leafy greens, broccoli, zucchini) for satiety."
        ]
        recovery = [
            "Prioritize 7.5 to 9 hours of uninterrupted sleep in a cool, dark room.",
            "Schedule 10 minutes of post-workout foam rolling on quads, glutes, and thoracic spine.",
            "Track daily resting heart rate; an elevated pulse suggests the need for extra rest.",
            "Take a contrast shower (cold/hot) to stimulate lymphatic flow and reduce inflammation."
        ]
    elif "muscle" in goal:
        plan_title = f"7-Day Hypertrophy & Strength Split ({profile.intensity} Intensity)"
        overview = f"Progressive overload resistance protocol built for {profile.user_name} targeting optimal muscle protein synthesis across all major muscle groups."
        days = [
            DayWorkoutPlan(
                day_number=1, day_title="Day 1: Chest & Triceps Push Focus", focus="Upper Body Push",
                warm_up="5 mins shoulder dislocations and dynamic push-ups",
                exercises=[
                    ExerciseItem(name="Dumbbell / Barbell Bench Press", sets="4", reps_or_duration="8-10 reps", rest_seconds="90s", target_muscle="Pectoralis Major", form_tip="Arch upper back, retract scapulae"),
                    ExerciseItem(name="Incline Dumbbell Press", sets="3", reps_or_duration="10-12 reps", rest_seconds="75s", target_muscle="Clavicular Head (Upper Chest)", form_tip="Lower under control for 3 seconds"),
                    ExerciseItem(name="Dips or Bench Dips", sets="3", reps_or_duration="10-12 reps", rest_seconds="60s", target_muscle="Triceps & Chest", form_tip="Do not flare elbows excessively"),
                    ExerciseItem(name="Overhead Triceps Extension", sets="3", reps_or_duration="12-15 reps", rest_seconds="60s", target_muscle="Triceps Long Head", form_tip="Keep upper arms locked vertically")
                ],
                cool_down="5 mins chest and triceps static stretch"
            ),
            DayWorkoutPlan(
                day_number=2, day_title="Day 2: Back & Biceps Pull Focus", focus="Upper Body Pull",
                warm_up="5 mins lat pull-aparts and arm circles",
                exercises=[
                    ExerciseItem(name="Pull-Ups or Lat Pulldowns", sets="4", reps_or_duration="8-10 reps", rest_seconds="90s", target_muscle="Latissimus Dorsi", form_tip="Pull chest to bar, drive elbows down"),
                    ExerciseItem(name="Bent-Over Barbell/Dumbbell Row", sets="3", reps_or_duration="8-10 reps", rest_seconds="90s", target_muscle="Rhomboids & Traps", form_tip="Hinge at hips, spine neutral"),
                    ExerciseItem(name="Incline Dumbbell Bicep Curls", sets="3", reps_or_duration="10-12 reps", rest_seconds="60s", target_muscle="Biceps Brachii", form_tip="Full stretch at bottom, no swinging"),
                    ExerciseItem(name="Hammer Curls", sets="3", reps_or_duration="12 reps", rest_seconds="60s", target_muscle="Brachialis & Forearms", form_tip="Neutral grip, squeeze at peak")
                ],
                cool_down="5 mins lat and forearm stretches"
            ),
            DayWorkoutPlan(
                day_number=3, day_title="Day 3: Lower Body Quad & Core Dominant", focus="Leg Hypertrophy",
                warm_up="5 mins hip mobility, deep goblet squats",
                exercises=[
                    ExerciseItem(name="Barbell / Goblet Squats", sets="4", reps_or_duration="8-10 reps", rest_seconds="120s", target_muscle="Quadriceps & Glutes", form_tip="Knees track over toes, hit parallel"),
                    ExerciseItem(name="Bulgarian Split Squats", sets="3", reps_or_duration="10 reps/leg", rest_seconds="75s", target_muscle="Quads & Stabilizers", form_tip="Elevate rear foot, torso slight forward lean"),
                    ExerciseItem(name="Leg Extensions or Sissy Squats", sets="3", reps_or_duration="12-15 reps", rest_seconds="60s", target_muscle="Rectus Femoris", form_tip="Squeeze quad hard at full lockout"),
                    ExerciseItem(name="Hanging Leg / Knee Raises", sets="3", reps_or_duration="12-15 reps", rest_seconds="45s", target_muscle="Lower Rectus Abdominis", form_tip="Curl pelvis upwards, do not swing")
                ],
                cool_down="5 mins quad and hip flexor stretches"
            ),
            DayWorkoutPlan(
                day_number=4, day_title="Day 4: Active Recovery & Deload", focus="Rest & Nervous System Reset",
                warm_up="10 mins light stroll",
                exercises=[],
                cool_down="Gentle stretching and mobility",
                is_rest_day=True
            ),
            DayWorkoutPlan(
                day_number=5, day_title="Day 5: Shoulders & Upper Back Hypertrophy", focus="Delts & Traps",
                warm_up="5 mins banded face pulls and internal/external rotations",
                exercises=[
                    ExerciseItem(name="Seated Dumbbell Shoulder Press", sets="4", reps_or_duration="8-10 reps", rest_seconds="90s", target_muscle="Anterior & Lateral Deltoids", form_tip="Press in gentle arc, stop ear-level"),
                    ExerciseItem(name="Dumbbell Lateral Raises", sets="4", reps_or_duration="12-15 reps", rest_seconds="60s", target_muscle="Lateral Deltoid", form_tip="Lead with elbows, pinkies slightly up"),
                    ExerciseItem(name="Face Pulls with Rope/Band", sets="3", reps_or_duration="15 reps", rest_seconds="60s", target_muscle="Rear Delts & Rotator Cuff", form_tip="Pull towards nose, externally rotate hands"),
                    ExerciseItem(name="Dumbbell Shrugs", sets="3", reps_or_duration="12 reps", rest_seconds="45s", target_muscle="Upper Trapezius", form_tip="Straight up and down, 2s squeeze at top")
                ],
                cool_down="5 mins neck and shoulder mobility"
            ),
            DayWorkoutPlan(
                day_number=6, day_title="Day 6: Posterior Chain & Hamstrings", focus="Glutes & Hamstrings",
                warm_up="5 mins glute bridges and hamstring sweeps",
                exercises=[
                    ExerciseItem(name="Romanian Deadlifts (RDL)", sets="4", reps_or_duration="8-10 reps", rest_seconds="90s", target_muscle="Hamstrings & Glutes", form_tip="Push hips back until deep stretch in hamstrings"),
                    ExerciseItem(name="Lying or Seated Leg Curls", sets="3", reps_or_duration="10-12 reps", rest_seconds="60s", target_muscle="Hamstring Flexors", form_tip="Control the negative descent"),
                    ExerciseItem(name="Calf Raises (Standing)", sets="4", reps_or_duration="15-20 reps", rest_seconds="45s", target_muscle="Gastrocnemius & Soleus", form_tip="Full stretch at bottom, hold peak 2s")
                ],
                cool_down="5 mins posterior chain stretches"
            ),
            DayWorkoutPlan(
                day_number=7, day_title="Day 7: Full Rest & Muscle Anabolism", focus="Tissue Synthesis",
                warm_up="Rest day",
                exercises=[],
                cool_down="Nutrient timing and adequate rest",
                is_rest_day=True
            )
        ]
        nutrition = [
            f"Eat in a slight caloric surplus (+250 to +400 kcal) to facilitate muscle protein synthesis.",
            f"Consume 2.0g to 2.2g protein per kg of bodyweight (~{int(profile.weight_kg * 2.1)}g daily), distributed across 4-5 meals.",
            "Incorporate 5g of Creatine Monohydrate daily with plenty of water for muscular power output.",
            "Prioritize post-workout nutrition: 30g fast-digesting protein + 50g simple/complex carbohydrates within 2 hours.",
            "Stay well hydrated with at least 3.5 liters of water daily to support cell volumization."
        ]
        recovery = [
            "Target 8 to 9 hours of quality sleep; human growth hormone peaks during deep REM/slow-wave sleep.",
            "Take active rest days seriously—muscle tissue grows during recovery, not inside the gym.",
            "Apply Epsom salt warm baths on heavy leg days to ease muscular tension and stiffness.",
            "Track weights and repetitions in a journal to ensure consistent weekly progressive overload."
        ]
    elif "flexibility" in goal:
        plan_title = f"7-Day Mobility & Functional Flexibility Flow ({profile.intensity} Intensity)"
        overview = f"Curated joint decompressive flows and neuromuscular stretching for {profile.user_name} to eliminate stiffness and enhance functional range of motion."
        days = [
            DayWorkoutPlan(
                day_number=1, day_title="Day 1: Hip Opener & Spine Decompression", focus="Pelvic & Spinal Mobility",
                warm_up="5 mins gentle cat-cow and pelvic tilts",
                exercises=[
                    ExerciseItem(name="90/90 Hip Switches", sets="3", reps_or_duration="10 reps/side", rest_seconds="30s", target_muscle="Hip Rotators & Capsule", form_tip="Keep chest tall, rotate smoothly through hips"),
                    ExerciseItem(name="Pigeon Pose Hold", sets="3", reps_or_duration="60 seconds/side", rest_seconds="30s", target_muscle="Piriformis & Glute Medius", form_tip="Breathe deeply, sink hips toward floor"),
                    ExerciseItem(name="World's Greatest Stretch", sets="3", reps_or_duration="8 reps/side", rest_seconds="30s", target_muscle="Thoracic Spine & Hamstrings", form_tip="Reach arm straight to ceiling with eyes tracking")
                ],
                cool_down="Child's pose with side reaches"
            ),
            DayWorkoutPlan(
                day_number=2, day_title="Day 2: Shoulder & Thoracic Unlock", focus="Upper Body Posture",
                warm_up="3 mins arm circles and gentle neck rolls",
                exercises=[
                    ExerciseItem(name="Thread the Needle Flow", sets="3", reps_or_duration="10 reps/side", rest_seconds="30s", target_muscle="Thoracic Rotators & Rhomboids", form_tip="Exhale as you twist deep through the ribcage"),
                    ExerciseItem(name="Doorway Chest Stretch", sets="3", reps_or_duration="45 seconds/side", rest_seconds="30s", target_muscle="Pec Minor & Biceps Tendon", form_tip="Step through gently until mild stretch"),
                    ExerciseItem(name="Wall Angels", sets="3", reps_or_duration="12 reps", rest_seconds="30s", target_muscle="Lower Trapezius & Postural Chain", form_tip="Keep wrists and elbows flat to wall")
                ],
                cool_down="Seated neck stretch with gentle hand assistance"
            ),
            DayWorkoutPlan(
                day_number=3, day_title="Day 3: Hamstring, Calf & Ankle Floss", focus="Lower Extremity Range",
                warm_up="3 mins ankle circles and calf raises",
                exercises=[
                    ExerciseItem(name="Downward Facing Dog Pedaling", sets="3", reps_or_duration="60 seconds", rest_seconds="30s", target_muscle="Calves & Hamstrings", form_tip="Press chest toward thighs"),
                    ExerciseItem(name="Single-Leg Kneeling Hamstring Fold", sets="3", reps_or_duration="45s/side", rest_seconds="30s", target_muscle="Semitendinosus & Biceps Femoris", form_tip="Flex front toes, fold from hip hinge"),
                    ExerciseItem(name="Knee-to-Wall Ankle Mobilization", sets="3", reps_or_duration="15 reps/ankle", rest_seconds="20s", target_muscle="Ankle Dorsiflexion", form_tip="Keep heel firmly pinned to ground")
                ],
                cool_down="Lying elevated legs-up-the-wall pose for 5 mins"
            ),
            DayWorkoutPlan(
                day_number=4, day_title="Day 4: Deep Rest & Yin Stillness", focus="Connective Tissue Release",
                warm_up="Slow nasal breathing",
                exercises=[
                    ExerciseItem(name="Butterfly Pose Hold", sets="2", reps_or_duration="3 minutes", rest_seconds="60s", target_muscle="Adductors & Groin", form_tip="Allow gravity to pull knees down gently"),
                    ExerciseItem(name="Supported Sphinx Pose", sets="2", reps_or_duration="3 minutes", rest_seconds="60s", target_muscle="Lumbar Spine", form_tip="Relax glutes completely, breathe softly")
                ],
                cool_down="Corpse pose (Savasana)",
                is_rest_day=True
            ),
            DayWorkoutPlan(
                day_number=5, day_title="Day 5: Full Body Vinyasa Flow", focus="Dynamic Flexibility & Balance",
                warm_up="Sun Salutation A (3 rounds)",
                exercises=[
                    ExerciseItem(name="Warrior II to Reverse Warrior", sets="3", reps_or_duration="5 breaths/side", rest_seconds="30s", target_muscle="Hip Flexors & Intercostals", form_tip="Deep front lunge, sink hips low"),
                    ExerciseItem(name="Cobra to Child's Pose Waves", sets="3", reps_or_duration="10 flow reps", rest_seconds="30s", target_muscle="Full Spine Articulation", form_tip="Ripple vertebra by vertebra"),
                    ExerciseItem(name="Standing Forward Fold with Toe Grab", sets="3", reps_or_duration="60 seconds", rest_seconds="30s", target_muscle="Posterior Fascial Line", form_tip="Slight bend in knees to protect lower back")
                ],
                cool_down="Supine spinal twist"
            ),
            DayWorkoutPlan(
                day_number=6, day_title="Day 6: Core Stability & Decompression", focus="Strength Through Range",
                warm_up="5 mins cat-cow and bird-dog",
                exercises=[
                    ExerciseItem(name="Side Plank with Arm Thread", sets="3", reps_or_duration="8 reps/side", rest_seconds="45s", target_muscle="Obliques & Serratus", form_tip="Lift hips high away from floor"),
                    ExerciseItem(name="Hollow Body Rock to Tuck", sets="3", reps_or_duration="30 seconds", rest_seconds="30s", target_muscle="Anterior Core", form_tip="Lower back glued to floor"),
                    ExerciseItem(name="Puppy Dog Pose (Heart Melting)", sets="3", reps_or_duration="60 seconds", rest_seconds="30s", target_muscle="Lats & Upper Chest", form_tip="Hips stacked directly over knees")
                ],
                cool_down="Child's pose with forehead rested"
            ),
            DayWorkoutPlan(
                day_number=7, day_title="Day 7: Rest & Mind-Body Integration", focus="Full Relaxation",
                warm_up="None",
                exercises=[],
                cool_down="15 minutes guided meditation or relaxed walk",
                is_rest_day=True
            )
        ]
        nutrition = [
            "Hydrate extensively: Fascial and tendon elasticity requires consistent cellular hydration (2.5 - 3.5L/day).",
            "Incorporate anti-inflammatory foods rich in Omega-3 fatty acids (salmon, walnuts, chia seeds).",
            "Consume collagen peptides (10-15g) paired with Vitamin C 30 mins prior to mobility sessions for ligament synthesis.",
            "Avoid excessive sodium and processed sugars that contribute to joint fluid retention and stiffness.",
            "Include turmeric and ginger teas for natural joint comfort and reduced soreness."
        ]
        recovery = [
            "Practice Box Breathing (4s inhale, 4s hold, 4s exhale, 4s hold) to activate parasympathetic recovery.",
            "Use warm hot water showers or sauna to loosen stiff connective tissues before stretching.",
            "Never bounce aggressively into a cold muscle (avoid ballistic stretching without warmup).",
            "Ensure 8 hours of sleep for cellular and collagen fiber remodeling."
        ]
    else: # General Wellness
        plan_title = f"7-Day Balanced Vitality & Functional Movement ({profile.intensity} Intensity)"
        overview = f"Harmonious blend of functional strength, aerobic cardiovascular conditioning, and mindful mobility built for {profile.user_name}."
        days = [
            DayWorkoutPlan(
                day_number=1, day_title="Day 1: Functional Full-Body Strength", focus="Compound Movements",
                warm_up="5 mins dynamic bodyweight lunges and arm circles",
                exercises=[
                    ExerciseItem(name="Goblet Squats", sets="3", reps_or_duration="12 reps", rest_seconds="60s", target_muscle="Quads & Core", form_tip="Keep elbows tucked inside knees"),
                    ExerciseItem(name="Dumbbell/Incline Push-Ups", sets="3", reps_or_duration="10-12 reps", rest_seconds="60s", target_muscle="Chest & Shoulders", form_tip="Full range of motion, avoid flaring elbows"),
                    ExerciseItem(name="Dumbbell Rows", sets="3", reps_or_duration="12 reps/arm", rest_seconds="60s", target_muscle="Upper Back", form_tip="Drive elbow toward hip bone"),
                    ExerciseItem(name="Plank with Shoulder Taps", sets="3", reps_or_duration="20 taps", rest_seconds="45s", target_muscle="Anti-Rotation Core", form_tip="Widen feet slightly to prevent hip sway")
                ],
                cool_down="5 mins hamstring and chest doorway stretch"
            ),
            DayWorkoutPlan(
                day_number=2, day_title="Day 2: Cardio & Outdoor Movement", focus="Cardiorespiratory Health",
                warm_up="3 mins brisk walk",
                exercises=[
                    ExerciseItem(name="Jogging / Cycling / Rowing", sets="1", reps_or_duration="30-35 mins", rest_seconds="None", target_muscle="Cardiovascular System", form_tip="Pace where conversational speech is possible"),
                    ExerciseItem(name="Jump Rope / Gentle Jumping", sets="3", reps_or_duration="60 seconds", rest_seconds="45s", target_muscle="Calves & Coordination", form_tip="Stay light on balls of feet")
                ],
                cool_down="5 mins gentle walking and calf stretching"
            ),
            DayWorkoutPlan(
                day_number=3, day_title="Day 3: Core & Lower Body Stability", focus="Balance & Glute Strength",
                warm_up="5 mins hip circles and glute bridges",
                exercises=[
                    ExerciseItem(name="Kettlebell/Dumbbell Deadlifts", sets="3", reps_or_duration="12 reps", rest_seconds="60s", target_muscle="Hamstrings & Lower Back", form_tip="Hinge hips back, spine straight as an arrow"),
                    ExerciseItem(name="Reverse Lunges", sets="3", reps_or_duration="10 reps/leg", rest_seconds="60s", target_muscle="Quads & Balance", form_tip="Step back smoothly without slamming knee"),
                    ExerciseItem(name="Side Plank Holds", sets="3", reps_or_duration="30s/side", rest_seconds="30s", target_muscle="Obliques", form_tip="Straight line from head to heels")
                ],
                cool_down="5 mins figure-four hip stretch"
            ),
            DayWorkoutPlan(
                day_number=4, day_title="Day 4: Active Recovery & Nature Walk", focus="Low Stress Restoration",
                warm_up="Morning outdoor walk",
                exercises=[
                    ExerciseItem(name="Relaxed Walk in Park", sets="1", reps_or_duration="30-45 mins", rest_seconds="None", target_muscle="Full Body & Mental Health", form_tip="Disconnect from screen devices, observe surroundings")
                ],
                cool_down="Gentle deep breathing in child's pose",
                is_rest_day=True
            ),
            DayWorkoutPlan(
                day_number=5, day_title="Day 5: Upper Body Sculpt & Posture", focus="Shoulders & Spinal Support",
                warm_up="5 mins band pull-aparts and arm swings",
                exercises=[
                    ExerciseItem(name="Dumbbell Overhead Press", sets="3", reps_or_duration="10 reps", rest_seconds="60s", target_muscle="Deltoids", form_tip="Lock in core, do not hyperextend lower spine"),
                    ExerciseItem(name="Lat Pulldowns or Resistance Band Pulls", sets="3", reps_or_duration="12 reps", rest_seconds="60s", target_muscle="Lats & Mid-Back", form_tip="Focus on squeezing shoulder blades"),
                    ExerciseItem(name="Bicep Curl to Hammer Curl", sets="3", reps_or_duration="10 reps", rest_seconds="45s", target_muscle="Biceps", form_tip="Control tempo: 2s up, 3s down")
                ],
                cool_down="5 mins upper back and neck stretches"
            ),
            DayWorkoutPlan(
                day_number=6, day_title="Day 6: Fun Cardio / Circuit Challenge", focus="Endurance & Energy",
                warm_up="5 mins full body mobilization",
                exercises=[
                    ExerciseItem(name="Bodyweight Squat to Calf Raise", sets="3", reps_or_duration="15 reps", rest_seconds="45s", target_muscle="Quads & Calves", form_tip="Explode up smoothly onto toes"),
                    ExerciseItem(name="Incline Bear Crawls", sets="3", reps_or_duration="30 seconds", rest_seconds="45s", target_muscle="Full Body & Shoulders", form_tip="Keep knees 2 inches off floor"),
                    ExerciseItem(name="Bird-Dog Crunches", sets="3", reps_or_duration="10 reps/side", rest_seconds="30s", target_muscle="Cross-Body Core", form_tip="Touch elbow to opposite knee smoothly")
                ],
                cool_down="5 mins full-body cooldown flow"
            ),
            DayWorkoutPlan(
                day_number=7, day_title="Day 7: Complete Rest & Renewal", focus="Restoration",
                warm_up="None",
                exercises=[],
                cool_down="Hydration and relaxation",
                is_rest_day=True
            )
        ]
        nutrition = [
            "Follow the 80/20 rule: 80% whole nutrient-dense foods, 20% flexible enjoyable choices.",
            "Eat a rainbow of colorful vegetables and fruits daily to maximize antioxidant and micronutrient intake.",
            "Aim for 25-35 grams of dietary fiber daily for optimal gut microbiome and blood sugar stability.",
            "Drink 2-3 liters of fresh water daily; start each morning with a large glass with a pinch of mineral salt.",
            "Prioritize lean proteins (eggs, poultry, tofu, Greek yogurt, fish) at every main meal."
        ]
        recovery = [
            "Maintain consistent sleep and wake schedules even on weekends (7.5 - 8.5 hours).",
            "Get 10-15 minutes of natural sunlight exposure within an hour of waking to set circadian rhythms.",
            "Incorporate 5-10 minutes of daily mindfulness, meditation, or prayer to lower cortisol.",
            "Listen to your joints: adjust weight or reps if discomfort arises."
        ]

    return GeneratedPlan(
        plan_id=str(uuid.uuid4()),
        user_name=profile.user_name,
        fitness_goal=profile.fitness_goal,
        intensity=profile.intensity,
        title=plan_title,
        overview=overview,
        days=days,
        nutrition_tips=nutrition,
        recovery_tips=recovery,
        created_at=datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
    )

def generate_workout_plan(profile: FitnessProfile) -> GeneratedPlan:
    """
    Generates a personalized 7-day workout plan using Gemini 2.5 Flash,
    validating the response and falling back gracefully if necessary.
    """
    if not GEMINI_API_KEY:
        # Graceful fallback with deterministic expert plan
        return get_default_fallback_plan(profile)

    prompt = f"""
Create a highly personalized, structured 7-Day Workout Plan and Nutrition/Recovery Guide for this individual:
User Name: {profile.user_name}
Age: {profile.age}
Gender: {profile.gender}
Weight: {profile.weight_kg} kg
Height: {profile.height_cm} cm
Fitness Level: {profile.fitness_level}
Primary Goal: {profile.fitness_goal}
Preferred Intensity: {profile.intensity}
Medical/Physical Limitations: {profile.medical_limitations}

Respond ONLY with valid JSON with the following exact keys:
{{
  "title": "Inspiring Plan Title",
  "overview": "2-3 sentences explaining how this plan addresses their goal and limitations",
  "days": [
    {{
      "day_number": 1,
      "day_title": "Day 1: Focus Area",
      "focus": "Focus description",
      "warm_up": "Warmup routine",
      "exercises": [
        {{
          "name": "Exercise Name",
          "sets": "e.g. 3",
          "reps_or_duration": "e.g. 10-12 reps or 40s",
          "rest_seconds": "e.g. 60s",
          "target_muscle": "Muscle group",
          "form_tip": "Key technique cue"
        }}
      ],
      "cool_down": "Cooldown routine",
      "is_rest_day": false
    }}
  ],
  "nutrition_tips": [
    "Tip 1 with actionable calorie/macro or hydration guidance",
    "Tip 2",
    "Tip 3",
    "Tip 4",
    "Tip 5"
  ],
  "recovery_tips": [
    "Tip 1 on sleep or tissue recovery",
    "Tip 2",
    "Tip 3",
    "Tip 4"
  ]
}}
Ensure exactly 7 days are provided (include 1-2 structured rest/recovery days appropriate for {profile.intensity} intensity).
"""

    try:
        response_dict = call_gemini_api(prompt=prompt, system_instruction=SYSTEM_INSTRUCTION, json_mode=True)
        raw_text = response_dict.get("raw_text", "")
        # Clean any accidental markdown code fences
        cleaned = raw_text.strip()
        if cleaned.startswith("```json"):
            cleaned = cleaned[7:]
        elif cleaned.startswith("```"):
            cleaned = cleaned[3:]
        if cleaned.endswith("```"):
            cleaned = cleaned[:-3]
        cleaned = cleaned.strip()

        parsed = json.loads(cleaned)
        
        # Parse into typed objects
        days_list: List[DayWorkoutPlan] = []
        for d in parsed.get("days", []):
            ex_list: List[ExerciseItem] = []
            for ex in d.get("exercises", []):
                ex_list.append(ExerciseItem(
                    name=str(ex.get("name", "Exercise")),
                    sets=str(ex.get("sets", "3")),
                    reps_or_duration=str(ex.get("reps_or_duration", "10 reps")),
                    rest_seconds=str(ex.get("rest_seconds", "60s")),
                    target_muscle=str(ex.get("target_muscle", "Target Area")),
                    form_tip=str(ex.get("form_tip", "Maintain good form"))
                ))
            days_list.append(DayWorkoutPlan(
                day_number=int(d.get("day_number", len(days_list) + 1)),
                day_title=str(d.get("day_title", f"Day {len(days_list) + 1}")),
                focus=str(d.get("focus", "Workout")),
                warm_up=str(d.get("warm_up", "5 mins dynamic stretches")),
                exercises=ex_list,
                cool_down=str(d.get("cool_down", "5 mins static stretches")),
                is_rest_day=bool(d.get("is_rest_day", False) or len(ex_list) == 0)
            ))

        return GeneratedPlan(
            plan_id=str(uuid.uuid4()),
            user_name=profile.user_name,
            fitness_goal=profile.fitness_goal,
            intensity=profile.intensity,
            title=str(parsed.get("title", f"7-Day {profile.fitness_goal} Plan")),
            overview=str(parsed.get("overview", "Personalized workout schedule created with FitBuddy AI.")),
            days=days_list if len(days_list) == 7 else get_default_fallback_plan(profile).days,
            nutrition_tips=[str(t) for t in parsed.get("nutrition_tips", [])] or get_default_fallback_plan(profile).nutrition_tips,
            recovery_tips=[str(t) for t in parsed.get("recovery_tips", [])] or get_default_fallback_plan(profile).recovery_tips,
            created_at=datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
        )
    except Exception as e:
        print(f"[FitBuddy Generator Error] {e}. Using deterministic fallback.")
        fallback = get_default_fallback_plan(profile)
        fallback.overview = f"{fallback.overview} (Generated via FitBuddy Offline Engine)"
        return fallback
