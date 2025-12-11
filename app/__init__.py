"""Backend Service Application"""

from app.config import settings
from app.database import init_db, get_session
from app.main import app

__version__ = "0.1.0"
__all__ = ["app", "settings", "init_db", "get_session"]
