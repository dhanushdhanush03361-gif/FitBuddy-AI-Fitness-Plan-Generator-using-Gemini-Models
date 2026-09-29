import json
from datetime import datetime
from typing import Dict, Any, List
from app.config import GEMINI_API_KEY
from app.schemas import GeneratedPlan, DayWorkoutPlan, ExerciseItem
from app.gemini_flash_generator import call_gemini_api
from app.gemini_generator import SYSTEM_INSTRUCTION

def update_workout_plan_with_feedback(current_plan_dict: dict, feedback_text: str) -> dict:
    """
    Submits user feedback and existing workout plan to Gemini to generate
    an updated, adjusted 7-day fitness regimen.
    """
    updated_plan = dict(current_plan_dict)
    updated_plan["updated_at"] = datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
    updated_plan["applied_feedback"] = feedback_text

    if not GEMINI_API_KEY:
        # Fallback local feedback refinement logic
        updated_plan["title"] = f"{current_plan_dict.get('title', 'Workout Plan')} (Refined)"
        updated_plan["overview"] = f"{current_plan_dict.get('overview', '')}\n\n[Applied Adjustment]: {feedback_text}"
        # Adjust some rest times or intensity notes to reflect feedback
        if "easier" in feedback_text.lower() or "too hard" in feedback_text.lower() or "tired" in feedback_text.lower():
            for day in updated_plan.get("days", []):
                for ex in day.get("exercises", []):
                    ex["sets"] = "2-3"
                    ex["rest_seconds"] = "75s-90s"
                    ex["form_tip"] = f"{ex.get('form_tip', '')} (Reduced tempo per feedback)"
        elif "harder" in feedback_text.lower() or "increase" in feedback_text.lower():
            for day in updated_plan.get("days", []):
                for ex in day.get("exercises", []):
                    ex["sets"] = "4"
                    ex["rest_seconds"] = "45s"
        return updated_plan

    prompt = f"""
Here is an existing 7-Day Workout Plan in JSON format:
{json.dumps(current_plan_dict)}

The user has tested this plan and submitted the following specific feedback:
"{feedback_text}"

Update and refine the 7-Day Workout Plan to specifically incorporate this feedback.
- Adjust exercise selection, sets, reps, duration, or rest periods as needed.
- If they report joint pain, fatigue, or schedule constraints, modify the affected days accordingly.
- Keep the exact JSON structure with keys: "title", "overview", "days" (each day with day_number, day_title, focus, warm_up, exercises, cool_down, is_rest_day), "nutrition_tips", "recovery_tips".
- Add a clear explanation in the "overview" describing the changes made based on the user's feedback.

Respond ONLY with valid JSON.
"""

    try:
        response_dict = call_gemini_api(prompt=prompt, system_instruction=SYSTEM_INSTRUCTION, json_mode=True)
        raw_text = response_dict.get("raw_text", "").strip()
        if raw_text.startswith("```json"):
            raw_text = raw_text[7:]
        elif raw_text.startswith("```"):
            raw_text = raw_text[3:]
        if raw_text.endswith("```"):
            raw_text = raw_text[:-3]
        raw_text = raw_text.strip()

        parsed = json.loads(raw_text)

        # Merge validated updates
        updated_plan["title"] = parsed.get("title", updated_plan.get("title", "Updated Plan"))
        updated_plan["overview"] = parsed.get("overview", updated_plan.get("overview", ""))
        if "days" in parsed and isinstance(parsed["days"], list) and len(parsed["days"]) > 0:
            updated_plan["days"] = parsed["days"]
        if "nutrition_tips" in parsed and isinstance(parsed["nutrition_tips"], list):
            updated_plan["nutrition_tips"] = parsed["nutrition_tips"]
        if "recovery_tips" in parsed and isinstance(parsed["recovery_tips"], list):
            updated_plan["recovery_tips"] = parsed["recovery_tips"]

        return updated_plan
    except Exception as e:
        print(f"[FitBuddy Feedback Refinement Error] {e}. Applying local refinement.")
        updated_plan["overview"] = f"{updated_plan.get('overview', '')}\n\n[Applied Adjustment]: {feedback_text}"
        return updated_plan
