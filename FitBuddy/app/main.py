from pathlib import Path
from fastapi import FastAPI, Request
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates

from app.routes import router

app = FastAPI(
    title="FitBuddy – AI Fitness Plan Generator",
    description="Generate structured 7-day workout routines, nutrition and recovery tips using Google Gemini.",
    version="1.0.0"
)

# CORS setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Static files mounting
base_dir = Path(__file__).resolve().parent.parent
static_dir = base_dir / "static"
if not static_dir.exists():
    static_dir = Path(__file__).resolve().parent.parent.parent / "static"

if static_dir.exists():
    app.mount("/static", StaticFiles(directory=str(static_dir)), name="static")

templates_dir = base_dir / "templates"
if not templates_dir.exists():
    templates_dir = Path(__file__).resolve().parent.parent.parent / "templates"

templates = Jinja2Templates(directory=str(templates_dir))

# Include application routes
app.include_router(router)

# 404 Exception Handler to prevent generic ugly 404 pages
@app.exception_handler(404)
async def custom_404_handler(request: Request, exc):
    return templates.TemplateResponse("error.html", {
        "request": request,
        "error_title": "404 - Page Not Found",
        "error_message": f"The requested URL '{request.url.path}' does not exist on FitBuddy.",
        "return_url": "/"
    }, status_code=404)

# 500 Exception Handler
@app.exception_handler(500)
async def custom_500_handler(request: Request, exc):
    return templates.TemplateResponse("error.html", {
        "request": request,
        "error_title": "500 - Server Error",
        "error_message": "An unexpected server error occurred. Please try again or return to home.",
        "return_url": "/"
    }, status_code=500)
