from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import structlog

from app.core.config import settings
from app.core.database import engine, Base
from app.api import api_router

structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.JSONRenderer()
    ],
    logger_factory=structlog.stdlib.LoggerFactory(),
)

logger = structlog.get_logger()

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Wolf AI Internal starting", version=settings.APP_VERSION)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield
    await engine.dispose()

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="Wolf AI Internal - Private Gemini-powered AI System",
    docs_url="/docs" if settings.ENVIRONMENT == "development" else None,
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:3001"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.exception_handler(Exception)
async def global_exception(request: Request, exc: Exception):
    logger.error("Unhandled exception", exc_info=exc)
    return JSONResponse(status_code=500, content={"error": "Internal error"})

app.include_router(api_router, prefix="/api/v1")

@app.get("/")
async def root():
    return {"name": settings.APP_NAME, "version": settings.APP_VERSION, "status": "running"}
