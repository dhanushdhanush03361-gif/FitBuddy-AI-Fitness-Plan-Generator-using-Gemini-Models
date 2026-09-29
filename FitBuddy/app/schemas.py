from typing import List, Optional
from pydantic import BaseModel, Field

class FitnessProfile(BaseModel):
    user_name: str = Field(default="Athlete", min_length=1, max_length=100)
    age: int = Field(default=25, ge=12, le=100)
    gender: str = Field(default="Not Specified")
    weight_kg: float = Field(default=70.0, ge=30.0, le=300.0)
    height_cm: float = Field(default=175.0, ge=100.0, le=250.0)
    fitness_level: str = Field(default="Beginner")  # Beginner, Intermediate, Advanced
    fitness_goal: str = Field(default="General Wellness")  # Weight Loss, Muscle Gain, General Wellness, Flexibility
    intensity: str = Field(default="Medium")  # Low, Medium, High
    medical_limitations: Optional[str] = Field(default="None")

class ExerciseItem(BaseModel):
    name: str
    sets: str
    reps_or_duration: str
    rest_seconds: str
    target_muscle: str
    form_tip: str

class DayWorkoutPlan(BaseModel):
    day_number: int
    day_title: str
    focus: str
    warm_up: str
    exercises: List[ExerciseItem]
    cool_down: str
    is_rest_day: bool = False

class GeneratedPlan(BaseModel):
    plan_id: str
    user_name: str
    fitness_goal: str
    intensity: str
    title: str
    overview: str
    days: List[DayWorkoutPlan]
    nutrition_tips: List[str]
    recovery_tips: List[str]
    created_at: str
    updated_at: Optional[str] = None
    applied_feedback: Optional[str] = None

class FeedbackSubmission(BaseModel):
    plan_id: str
    feedback_text: str = Field(..., min_length=2, max_length=1000)

class HealthStatus(BaseModel):
    status: str
    gemini_key_configured: bool
    model: str
    database_ready: bool
    timestamp: str
