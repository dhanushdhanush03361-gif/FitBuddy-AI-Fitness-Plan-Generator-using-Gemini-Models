import sys
import os
from pathlib import Path

# Add root and FitBuddy to python module search path
root_dir = Path(__file__).resolve().parent.parent
fitbuddy_dir = root_dir / "FitBuddy"
if str(root_dir) not in sys.path:
    sys.path.insert(0, str(root_dir))
if str(fitbuddy_dir) not in sys.path:
    sys.path.insert(0, str(fitbuddy_dir))

try:
    from FitBuddy.app.main import app
except ImportError:
    from app.main import app

handler = app
