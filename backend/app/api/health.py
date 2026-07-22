from fastapi import APIRouter
from app.core.config import settings
from app.services.gemini_service import gemini_service

router = APIRouter()

@router.get("/")
async def health():
    return {
        "status": "healthy",
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "gemini_configured": gemini_service.configured
    }

@router.get("/gemini")
async def gemini_health():
    return {"configured": gemini_service.configured, "model": settings.GEMINI_MODEL}
