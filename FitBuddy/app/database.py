import os
import json
import sqlite3
from typing import List, Optional, Dict
from datetime import datetime
from app.config import DATABASE_URL, ENVIRONMENT

# Global in-memory fallback cache for serverless environments
_MEMORY_CACHE: Dict[str, dict] = {}

def get_db_path() -> str:
    """Determine a valid SQLite path depending on environment."""
    if DATABASE_URL.startswith("sqlite:///"):
        raw_path = DATABASE_URL.replace("sqlite:///", "")
        # In serverless environments like Vercel, root filesystem is read-only except /tmp
        if os.environ.get("VERCEL") or ENVIRONMENT == "production":
            return "/tmp/fitbuddy.db"
        return raw_path
    elif DATABASE_URL.startswith("sqlite://"):
        return "/tmp/fitbuddy.db"
    return "/tmp/fitbuddy.db"

def init_db():
    """Initializes the database schema."""
    db_path = get_db_path()
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS workout_plans (
                plan_id TEXT PRIMARY KEY,
                user_name TEXT,
                fitness_goal TEXT,
                intensity TEXT,
                plan_data TEXT,
                created_at TEXT,
                updated_at TEXT,
                feedback_history TEXT
            )
        """)
        conn.commit()
        conn.close()
    except Exception as e:
        print(f"[FitBuddy DB Warning] Could not init sqlite at {db_path}: {e}. Falling back to memory storage.")

def save_plan(plan_dict: dict) -> bool:
    """Saves or updates a workout plan."""
    plan_id = plan_dict.get("plan_id")
    if not plan_id:
        return False
    
    # Always keep in-memory cache updated
    _MEMORY_CACHE[plan_id] = plan_dict

    db_path = get_db_path()
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO workout_plans (plan_id, user_name, fitness_goal, intensity, plan_data, created_at, updated_at, feedback_history)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(plan_id) DO UPDATE SET
                plan_data = excluded.plan_data,
                updated_at = excluded.updated_at,
                feedback_history = excluded.feedback_history
        """, (
            plan_id,
            plan_dict.get("user_name", "Athlete"),
            plan_dict.get("fitness_goal", "General Wellness"),
            plan_dict.get("intensity", "Medium"),
            json.dumps(plan_dict),
            plan_dict.get("created_at", datetime.utcnow().isoformat()),
            plan_dict.get("updated_at", datetime.utcnow().isoformat()),
            plan_dict.get("applied_feedback", "")
        ))
        conn.commit()
        conn.close()
        return True
    except Exception as e:
        print(f"[FitBuddy DB Warning] SQLite write failed: {e}. Saved to memory cache.")
        return True

def get_plan(plan_id: str) -> Optional[dict]:
    """Retrieves a workout plan by plan_id."""
    if plan_id in _MEMORY_CACHE:
        return _MEMORY_CACHE[plan_id]

    db_path = get_db_path()
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT plan_data FROM workout_plans WHERE plan_id = ?", (plan_id,))
        row = cursor.fetchone()
        conn.close()
        if row and row[0]:
            data = json.loads(row[0])
            _MEMORY_CACHE[plan_id] = data
            return data
    except Exception as e:
        print(f"[FitBuddy DB Warning] SQLite read failed: {e}")

    return None

def list_all_plans() -> List[dict]:
    """Lists all saved workout plans."""
    plans = []
    seen_ids = set()

    db_path = get_db_path()
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT plan_id, user_name, fitness_goal, intensity, created_at, updated_at FROM workout_plans ORDER BY created_at DESC")
        rows = cursor.fetchall()
        conn.close()
        for r in rows:
            seen_ids.add(r[0])
            plans.append({
                "plan_id": r[0],
                "user_name": r[1],
                "fitness_goal": r[2],
                "intensity": r[3],
                "created_at": r[4],
                "updated_at": r[5]
            })
    except Exception as e:
        print(f"[FitBuddy DB Warning] SQLite list failed: {e}")

    # Add any from memory cache not yet in sqlite list
    for pid, p in _MEMORY_CACHE.items():
        if pid not in seen_ids:
            plans.append({
                "plan_id": pid,
                "user_name": p.get("user_name", "Athlete"),
                "fitness_goal": p.get("fitness_goal", "General Wellness"),
                "intensity": p.get("intensity", "Medium"),
                "created_at": p.get("created_at", ""),
                "updated_at": p.get("updated_at", "")
            })

    return plans

# Auto-initialize table on import
init_db()
