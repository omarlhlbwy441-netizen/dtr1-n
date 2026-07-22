from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, Integer as SAInt, Float
from sqlalchemy.sql import func
from app.core.database import Base

class Message(Base):
    __tablename__ = "messages"

    id = Column(Integer, primary_key=True, index=True)
    conversation_id = Column(Integer, ForeignKey("conversations.id"), nullable=False)

    role = Column(String(20), nullable=False)  # user, assistant, system
    content = Column(Text, nullable=False)

    # AI Metadata
    tokens_used = Column(SAInt, default=0)
    latency_ms = Column(Float)
    model_version = Column(String(100))

    # RAG Sources
    sources = Column(JSON, default=list)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
