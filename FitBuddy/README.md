# FitBuddy – AI Fitness Plan Generator using Gemini Models

FitBuddy is an intelligent, full-stack fitness planning application powered by Google Gemini 2.5 Flash, FastAPI, and Jetpack Compose. It generates personalized 7-day workout plans, targeted nutrition advice, and recovery protocols based on individual fitness levels, goals, intensities, and physical limitations.

---

## ⚡ Features

1. **Fitness Assessment Form**:
   - Collects user statistics: Age, gender, weight (kg), height (cm), training experience.
   - Goals: **Weight Loss**, **Muscle Gain**, **General Wellness**, and **Flexibility**.
   - Intensities: **Low**, **Medium**, **High**.
   - Injury & equipment notes to prevent biomechanical strain.
2. **AI-Powered 7-Day Workout Routine**:
   - Daily structured exercises with precise sets, reps/duration, rest times, target muscles, and form tips.
   - Warm-up and cool-down routines for every active day.
   - Tailored rest and active recovery days.
3. **Personalized Nutrition & Recovery Tips**:
   - Caloric goals, protein targets, hydration protocols, and sleep hygiene.
4. **Adaptive Feedback Loop**:
   - Submit real-time feedback (e.g., "swap dumbbell press for pushups", "too intense on day 3") to iteratively update the plan.
5. **Admin Dashboard**:
   - View all registered plans at `/view-all-users` protected by `ADMIN_TOKEN`.
6. **Robust Error Handling**:
   - Zero-crash fallback engine for offline execution or unconfigured API keys.
   - Custom 404 and 500 error pages.
   - Built-in `/health` diagnostic endpoint.

---

## 🚀 Environment Variables

Configure these in `.env` (local) or Vercel Environment Variables:

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `GEMINI_API_KEY` | Google AI Studio API key | *(From AI Studio Secrets)* |
| `AI_PROVIDER` | AI service provider | `gemini` |
| `WORKOUT_MODEL` | Gemini model for workouts | `gemini-2.5-flash` |
| `FLASH_MODEL` | Gemini Flash model name | `gemini-2.5-flash` |
| `DATABASE_URL` | Database connection string | `sqlite:///./fitbuddy.db` |
| `ADMIN_TOKEN` | Secret token to access `/view-all-users` | `fitbuddy-admin-secret-2026` |
| `ENVIRONMENT` | Environment mode | `production` or `development` |

---

## 💻 Local Run Instructions (VS Code / Terminal)

```bash
# 1. Create and activate a virtual environment
python3 -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Create .env file with your GEMINI_API_KEY
cp .env.example .env

# 4. Start the FastAPI development server
uvicorn app.main:app --reload --port 8000

# 5. Open in browser
# App: http://localhost:8000
# Admin: http://localhost:8000/view-all-users?token=fitbuddy-admin-secret-2026
# Health: http://localhost:8000/health
# Docs: http://localhost:8000/docs
```

---

## ☁️ Vercel Deployment Instructions

1. **Push to GitHub**:
   - Commit all project files and push to your GitHub repository.
2. **Import to Vercel**:
   - Go to [vercel.com](https://vercel.com) and click **Add New > Project**.
   - Select your GitHub repository.
3. **Configure Environment Variables**:
   - In the Vercel project settings, add:
     - `GEMINI_API_KEY` = your Google AI Studio API key
     - `ADMIN_TOKEN` = `fitbuddy-admin-secret-2026` (or your chosen secret)
     - `AI_PROVIDER` = `gemini`
     - `WORKOUT_MODEL` = `gemini-2.5-flash`
4. **Deploy**:
   - Click **Deploy**. Vercel will build `@vercel/python` using `api/index.py` and `vercel.json`.
5. **Verify**:
   - Open your deployed Vercel URL (e.g., `https://your-fitbuddy.vercel.app/`).
   - Test generating a plan, submitting feedback, and checking `/health`.

---

## 📱 Android App (Jetpack Compose)

This project also includes the complete native Android Jetpack Compose version of FitBuddy in `/app`, featuring Material 3 theming, Room offline persistence, and direct Gemini 2.5 Flash integration.
