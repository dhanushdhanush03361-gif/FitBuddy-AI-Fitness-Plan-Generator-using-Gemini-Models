import sys
from pathlib import Path
import pytest
from fastapi.testclient import TestClient

root_dir = Path(__file__).resolve().parent.parent
if str(root_dir) not in sys.path:
    sys.path.insert(0, str(root_dir))

from app.main import app
from app.config import ADMIN_TOKEN

client = TestClient(app)

def test_home_page_loads():
    """Verify home page loads successfully with 200 OK and expected HTML."""
    response = client.get("/")
    assert response.status_code == 200
    assert "FitBuddy" in response.text
    assert "Fitness Assessment" in response.text
    assert "Primary Fitness Goal" in response.text

def test_health_endpoint():
    """Verify GET /health returns valid JSON with health status."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "model" in data
    assert "database_ready" in data
    assert data["database_ready"] is True

def test_generate_workout_endpoint():
    """Verify POST /generate-workout accepts profile form data and returns plan."""
    payload = {
        "user_name": "Jordan",
        "age": 27,
        "gender": "Female",
        "weight_kg": 62.0,
        "height_cm": 168.0,
        "fitness_level": "Intermediate",
        "fitness_goal": "Weight Loss",
        "intensity": "High",
        "medical_limitations": "None"
    }
    response = client.post("/generate-workout", data=payload)
    assert response.status_code == 200
    assert "7-Day Training Schedule" in response.text
    assert "Jordan" in response.text
    assert "Nutrition Strategy" in response.text

def test_generate_workout_json_api():
    """Verify POST /generate-workout supports JSON API clients."""
    payload = {
        "user_name": "Sam",
        "age": 30,
        "gender": "Male",
        "weight_kg": 80.0,
        "height_cm": 180.0,
        "fitness_level": "Beginner",
        "fitness_goal": "Muscle Gain",
        "intensity": "Medium",
        "medical_limitations": "No squats"
    }
    response = client.post("/generate-workout", data=payload, headers={"Accept": "application/json"})
    assert response.status_code == 200
    data = response.json()
    assert "plan_id" in data
    assert len(data["days"]) == 7
    assert len(data["nutrition_tips"]) > 0

def test_submit_feedback_refinement():
    """Verify POST /submit-feedback refines an existing plan."""
    # First generate a plan to get a valid plan_id
    gen_res = client.post("/generate-workout", data={
        "user_name": "Taylor",
        "fitness_goal": "Flexibility",
        "intensity": "Low"
    }, headers={"Accept": "application/json"})
    plan_id = gen_res.json()["plan_id"]

    feedback_payload = {
        "plan_id": plan_id,
        "feedback_text": "Reduce morning stretch duration and focus more on hips."
    }
    feedback_res = client.post("/submit-feedback", data=feedback_payload)
    assert feedback_res.status_code == 200
    assert "Plan Refined Successfully" in feedback_res.text or "Taylor" in feedback_res.text

def test_admin_endpoint_security():
    """Verify GET /view-all-users is protected by ADMIN_TOKEN."""
    unauthorized_res = client.get("/view-all-users")
    assert unauthorized_res.status_code == 401
    assert "Protected Admin Access" in unauthorized_res.text

    authorized_res = client.get(f"/view-all-users?token={ADMIN_TOKEN}")
    assert authorized_res.status_code == 200
    assert "All Registered Plans" in authorized_res.text

def test_custom_404_handler():
    """Verify non-existent route returns clean 404 page instead of raw crash."""
    response = client.get("/non-existent-route-xyz")
    assert response.status_code == 404
    assert "404 - Page Not Found" in response.text
