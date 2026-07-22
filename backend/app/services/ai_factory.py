from app.core.config import settings
from typing import Union

class AIFactory:
    """
    AI Factory Pattern
    Switches between Gemini (Cloud) and Local AI (Private)
    """

    _gemini_service = None
    _local_service = None

    @staticmethod
    def get_service():
        """Get the appropriate AI service based on configuration"""
        provider = getattr(settings, "AI_PROVIDER", "gemini").lower()

        if provider == "local":
            from .local_ai_service import LocalAIService
            if AIFactory._local_service is None:
                AIFactory._local_service = LocalAIService()
            return AIFactory._local_service

        elif provider == "gemini":
            from .gemini_service import GeminiService
            if AIFactory._gemini_service is None:
                AIFactory._gemini_service = GeminiService()
            return AIFactory._gemini_service

        else:
            raise ValueError(f"Unknown AI provider: {provider}. Use 'local' or 'gemini'.")

    @staticmethod
    def get_provider_name() -> str:
        """Get current provider name"""
        return getattr(settings, "AI_PROVIDER", "gemini").lower()

    @staticmethod
    def is_local() -> bool:
        """Check if using local AI"""
        return AIFactory.get_provider_name() == "local"

    @staticmethod
    def is_gemini() -> bool:
        """Check if using Gemini"""
        return AIFactory.get_provider_name() == "gemini"

    @staticmethod
    async def health_check() -> dict:
        """Check health of current AI provider"""
        try:
            service = AIFactory.get_service()
            if hasattr(service, 'health_check'):
                return await service.health_check()
            return {"status": "unknown", "provider": AIFactory.get_provider_name()}
        except Exception as e:
            return {"status": "error", "error": str(e), "provider": AIFactory.get_provider_name()}
