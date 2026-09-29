import json
import logging
import requests
from typing import Dict, Any, Optional
from app.config import GEMINI_API_KEY, FLASH_MODEL

logger = logging.getLogger("fitbuddy.gemini_flash")

GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

def call_gemini_api(prompt: str, system_instruction: Optional[str] = None, json_mode: bool = True) -> Dict[str, Any]:
    """
    Direct REST API client for Google Gemini 2.5 Flash.
    Conforms to the official Google Gemini API specification.
    """
    if not GEMINI_API_KEY:
        raise ValueError("GEMINI_API_KEY is not configured in environment variables.")

    model = FLASH_MODEL or "gemini-2.5-flash"
    url = f"{GEMINI_BASE_URL}/{model}:generateContent?key={GEMINI_API_KEY}"

    headers = {
        "Content-Type": "application/json"
    }

    generation_config: Dict[str, Any] = {
        "temperature": 0.4,
        "topP": 0.9,
    }

    if json_mode:
        generation_config["responseMimeType"] = "application/json"

    payload: Dict[str, Any] = {
        "contents": [
            {
                "role": "user",
                "parts": [{"text": prompt}]
            }
        ],
        "generationConfig": generation_config
    }

    if system_instruction:
        payload["systemInstruction"] = {
            "parts": [{"text": system_instruction}]
        }

    try:
        response = requests.post(url, headers=headers, json=payload, timeout=60)
    except requests.exceptions.Timeout:
        raise RuntimeError("Google Gemini API request timed out after 60 seconds.")
    except requests.exceptions.RequestException as e:
        raise RuntimeError(f"Network error communicating with Gemini API: {str(e)}")

    if response.status_code == 429:
        raise RuntimeError("Google Gemini API rate limit or quota exceeded. Please try again shortly.")
    elif response.status_code == 400:
        error_detail = response.json().get("error", {}).get("message", response.text)
        raise RuntimeError(f"Gemini API Bad Request (400): {error_detail}")
    elif response.status_code != 200:
        error_detail = response.json().get("error", {}).get("message", response.text)
        raise RuntimeError(f"Gemini API returned HTTP {response.status_code}: {error_detail}")

    data = response.json()
    candidates = data.get("candidates", [])
    if not candidates:
        raise RuntimeError("Gemini returned an empty candidate list.")

    content = candidates[0].get("content", {})
    parts = content.get("parts", [])
    if not parts or "text" not in parts[0]:
        raise RuntimeError("Gemini returned a response without text content.")

    raw_text = parts[0]["text"].strip()
    return {"raw_text": raw_text, "model": model}
