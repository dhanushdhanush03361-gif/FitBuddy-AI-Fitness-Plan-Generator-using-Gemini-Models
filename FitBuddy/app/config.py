import os
from pathlib import Path

# Load .env file if present in the project directory
env_path = Path(__file__).resolve().parent.parent / ".env"
if not env_path.exists():
    env_path = Path(__file__).resolve().parent.parent.parent / ".env"

if env_path.exists():
    try:
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    key, val = line.split("=", 1)
                    key = key.strip()
                    val = val.strip().strip("'\"")
                    if key and key not in os.environ:
                        os.environ[key] = val
    except Exception:
        pass

GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "").strip()
AI_PROVIDER: str = os.getenv("AI_PROVIDER", "gemini").strip()
WORKOUT_MODEL: str = os.getenv("WORKOUT_MODEL", "gemini-2.5-flash").strip()
FLASH_MODEL: str = os.getenv("FLASH_MODEL", "gemini-2.5-flash").strip()
DATABASE_URL: str = os.getenv("DATABASE_URL", "sqlite:///./fitbuddy.db").strip()
ADMIN_TOKEN: str = os.getenv("ADMIN_TOKEN", "fitbuddy-admin-secret-2026").strip()
ENVIRONMENT: str = os.getenv("ENVIRONMENT", "production").strip()
