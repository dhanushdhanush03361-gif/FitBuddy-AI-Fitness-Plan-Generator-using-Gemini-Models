import os
from pathlib import Path
from datetime import datetime
from typing import Optional
from fastapi import APIRouter, Request, Form, Depends, HTTPException, Query, status
from fastapi.responses import HTMLResponse, JSONResponse, RedirectResponse
from fastapi.templating import Jinja2Templates

from app.config import GEMINI_API_KEY, FLASH_MODEL, ADMIN_TOKEN
from app.schemas import FitnessProfile, HealthStatus
from app.gemini_generator import generate_workout_plan
from app.updated_plan import update_workout_plan_with_feedback
from app.database import save_plan, get_plan, list_all_plans

# Setup Jinja2 templates directory with multiple candidate paths for local & Vercel
base_dir = Path(__file__).resolve().parent.parent
templates_dir = base_dir / "templates"
if not templates_dir.exists():
    templates_dir = Path(__file__).resolve().parent.parent.parent / "templates"

templates = Jinja2Templates(directory=str(templates_dir))

router = APIRouter()

@router.get("/", response_class=HTMLResponse)
async def home_page(request: Request):
    """Renders the FitBuddy interactive home page."""
    return templates.TemplateResponse("index.html", {
        "request": request,
        "api_key_configured": bool(GEMINI_API_KEY),
        "model": FLASH_MODEL
    })

@router.post("/generate-workout", response_class=HTMLResponse)
async def generate_workout_endpoint(
    request: Request,
    user_name: str = Form(default="Athlete"),
    age: int = Form(default=25),
    gender: str = Form(default="Not Specified"),
    weight_kg: float = Form(default=70.0),
    height_cm: float = Form(default=175.0),
    fitness_level: str = Form(default="Beginner"),
    fitness_goal: str = Form(default="General Wellness"),
    intensity: str = Form(default="Medium"),
    medical_limitations: Optional[str] = Form(default="None")
):
    """Accepts fitness profile inputs and generates the 7-day personalized workout plan."""
    # Basic input sanitation
    user_name = user_name.strip() or "Athlete"
    age = max(12, min(age, 100))
    weight_kg = max(30.0, min(weight_kg, 300.0))
    height_cm = max(100.0, min(height_cm, 250.0))

    profile = FitnessProfile(
        user_name=user_name,
        age=age,
        gender=gender,
        weight_kg=weight_kg,
        height_cm=height_cm,
        fitness_level=fitness_level,
        fitness_goal=fitness_goal,
        intensity=intensity,
        medical_limitations=medical_limitations or "None"
    )

    try:
        plan = generate_workout_plan(profile)
        plan_dict = plan.model_dump()
        save_plan(plan_dict)

        # If client requested JSON (API client), respond with JSON
        if "application/json" in request.headers.get("accept", ""):
            return JSONResponse(content=plan_dict)

        return templates.TemplateResponse("result.html", {
            "request": request,
            "plan": plan_dict,
            "api_key_configured": bool(GEMINI_API_KEY)
        })
    except Exception as e:
        return templates.TemplateResponse("error.html", {
            "request": request,
            "error_title": "Plan Generation Error",
            "error_message": f"Unable to generate plan: {str(e)}",
            "return_url": "/"
        }, status_code=500)

@router.post("/submit-feedback", response_class=HTMLResponse)
async def submit_feedback_endpoint(
    request: Request,
    plan_id: str = Form(...),
    feedback_text: str = Form(...)
):
    """Submits feedback and refines the current workout plan using Gemini."""
    feedback_text = feedback_text.strip()
    if not feedback_text:
        return templates.TemplateResponse("error.html", {
            "request": request,
            "error_title": "Invalid Feedback",
            "error_message": "Please enter specific feedback on what you would like adjusted.",
            "return_url": f"/plan/{plan_id}"
        }, status_code=400)

    current_plan = get_plan(plan_id)
    if not current_plan:
        return templates.TemplateResponse("error.html", {
            "request": request,
            "error_title": "Plan Not Found",
            "error_message": f"Could not find existing plan with ID: {plan_id}",
            "return_url": "/"
        }, status_code=404)

    try:
        updated = update_workout_plan_with_feedback(current_plan, feedback_text)
        save_plan(updated)

        if "application/json" in request.headers.get("accept", ""):
            return JSONResponse(content=updated)

        return templates.TemplateResponse("result.html", {
            "request": request,
            "plan": updated,
            "api_key_configured": bool(GEMINI_API_KEY),
            "just_updated": True
        })
    except Exception as e:
        return templates.TemplateResponse("error.html", {
            "request": request,
            "error_title": "Refinement Error",
            "error_message": f"Failed to update workout plan: {str(e)}",
            "return_url": f"/plan/{plan_id}"
        }, status_code=500)

@router.get("/plan/{plan_id}", response_class=HTMLResponse)
async def view_plan_by_id(request: Request, plan_id: str):
    """Direct permalink view for any saved plan."""
    plan = get_plan(plan_id)
    if not plan:
        return templates.TemplateResponse("error.html", {
            "request": request,
            "error_title": "Plan Not Found",
            "error_message": f"No workout plan exists with ID: {plan_id}",
            "return_url": "/"
        }, status_code=404)

    return templates.TemplateResponse("result.html", {
        "request": request,
        "plan": plan,
        "api_key_configured": bool(GEMINI_API_KEY)
    })

@router.get("/view-all-users", response_class=HTMLResponse)
async def view_all_users_admin(
    request: Request,
    token: Optional[str] = Query(default=None)
):
    """
    Protected Admin route to view all registered users and generated plans.
    Requires token matching ADMIN_TOKEN or ?token=fitbuddy-admin-secret-2026.
    """
    auth_header = request.headers.get("Authorization", "")
    bearer_token = auth_header.replace("Bearer ", "").strip() if "Bearer " in auth_header else ""

    provided_token = token or bearer_token

    # Allow if token matches or if token not set in production fallback
    is_authorized = bool(provided_token and provided_token == ADMIN_TOKEN)

    if not is_authorized:
        return templates.TemplateResponse("all_users.html", {
            "request": request,
            "authorized": False,
            "plans": [],
            "error": "Admin access token required. Provide valid token via ?token=..."
        }, status_code=401)

    plans = list_all_plans()
    return templates.TemplateResponse("all_users.html", {
        "request": request,
        "authorized": True,
        "plans": plans,
        "admin_token": ADMIN_TOKEN
    })

@router.get("/health", response_model=HealthStatus)
async def health_check():
    """Health diagnostic endpoint returning JSON system status."""
    return HealthStatus(
        status="healthy",
        gemini_key_configured=bool(GEMINI_API_KEY),
        model=FLASH_MODEL,
        database_ready=True,
        timestamp=datetime.utcnow().isoformat()
    )
