from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, JSON
from sqlalchemy.sql import func
from app.core.database import Base

class Conversation(Base):
    __tablename__ = "conversations"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)

    title = Column(String(255))
    system_prompt = Column(Text, default="You are Wolf AI, a helpful internal assistant.")

    # Knowledge Base attached
    knowledge_base_id = Column(Integer, ForeignKey("knowledge_bases.id"), nullable=True)

    # Settings
    model = Column(String(100), default="gemini-1.5-pro")
    temperature = Column(String(10), default="0.7")

    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
