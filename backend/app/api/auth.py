from fastapi import APIRouter, Depends, HTTPException, Request
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from datetime import timedelta

from app.core.database import get_db
from app.core.security import verify_password, get_password_hash, create_access_token, decode_token
from app.models.user import User
from app.services.gemini_service import gemini_service

router = APIRouter()
security = HTTPBearer()

async def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security), db: AsyncSession = Depends(get_db)) -> User:
    payload = decode_token(credentials.credentials)
    user_id = payload.get("sub")
    result = await db.execute(select(User).where(User.id == int(user_id)))
    user = result.scalar_one_or_none()
    if not user or not user.is_active:
        raise HTTPException(status_code=401, detail="Invalid user")
    return user

async def get_admin(user: User = Depends(get_current_user)) -> User:
    if user.role != "admin":
        raise HTTPException(status_code=403, detail="Admin required")
    return user

@router.post("/register")
async def register(data: dict, db: AsyncSession = Depends(get_db)):
    email = data.get("email")
    password = data.get("password")
    username = data.get("username", email.split("@")[0])

    result = await db.execute(select(User).where(User.email == email))
    if result.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Email exists")

    user = User(
        email=email,
        username=username,
        hashed_password=get_password_hash(password),
        full_name=data.get("full_name")
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)

    token = create_access_token({"sub": str(user.id), "email": user.email})
    return {"access_token": token, "token_type": "bearer", "user": {"id": user.id, "email": user.email, "role": user.role}}

@router.post("/login")
async def login(data: dict, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.email == data.get("email")))
    user = result.scalar_one_or_none()

    if not user or not verify_password(data.get("password"), user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    token = create_access_token({"sub": str(user.id), "email": user.email, "role": user.role})
    return {"access_token": token, "token_type": "bearer", "user": {"id": user.id, "email": user.email, "role": user.role}}

@router.get("/me")
async def get_me(current_user: User = Depends(get_current_user)):
    return {"id": current_user.id, "email": current_user.email, "username": current_user.username, "role": current_user.role, "tokens_used": current_user.tokens_used, "tokens_limit": current_user.tokens_limit}
