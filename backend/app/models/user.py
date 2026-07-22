from sqlalchemy import Column, Integer, String, Boolean, DateTime, Text, JSON
from sqlalchemy.sql import func
from app.core.database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(255), unique=True, index=True, nullable=False)
    username = Column(String(100), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    full_name = Column(String(255))
    role = Column(String(50), default="user")  # admin, user, viewer
    is_active = Column(Boolean, default=True)

    # AI Usage
    tokens_used = Column(Integer, default=0)
    tokens_limit = Column(Integer, default=50000)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
