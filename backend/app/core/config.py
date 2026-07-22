from pydantic_settings import BaseSettings
from functools import lru_cache

class Settings(BaseSettings):
    # ═══ App Info ═══
    APP_NAME: str = "رفيق | Rafeeq"
    APP_VERSION: str = "2.0.0"
    ENVIRONMENT: str = "development"

    # ═══ Database ═══
    DATABASE_URL: str = "postgresql+asyncpg://wolf_ai:YOUR_DB_PASSWORD@localhost:5432/wolf_ai_internal"
    REDIS_URL: str = "redis://localhost:6379/0"
    REDIS_PASSWORD: str = ""
    CHROMA_URL: str = "http://localhost:8000"

    # ═══ AI Provider Selection ═══
    AI_PROVIDER: str = "gemini"  # "gemini" | "local"

    # ═══ Google Gemini (Cloud) ═══
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-1.5-pro"

    # ═══ Local AI (Ollama) ═══
    OLLAMA_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "qwen2.5:14b"

    # ═══ Security ═══
    SECRET_KEY: str = "YOUR_SECRET_KEY"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    ALGORITHM: str = "HS256"

    # ═══ RAG Settings ═══
    RAG_CHUNK_SIZE: int = 1000
    RAG_CHUNK_OVERLAP: int = 200
    RAG_TOP_K: int = 5

    # ═══ Rate Limiting ═══
    RATE_LIMIT_REQUESTS: int = 100
    RATE_LIMIT_WINDOW: int = 60

    class Config:
        env_file = ".env"

@lru_cache()
def get_settings() -> Settings:
    return Settings()

settings = get_settings()
