from sqlalchemy import Column, Integer, String, Text, DateTime, ForeignKey, JSON, Boolean
from sqlalchemy.sql import func
from app.core.database import Base

class KnowledgeBase(Base):
    __tablename__ = "knowledge_bases"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)

    name = Column(String(255), nullable=False)
    description = Column(Text)

    # ChromaDB Collection Name
    collection_name = Column(String(100), unique=True, nullable=False)

    # Documents count
    document_count = Column(Integer, default=0)

    # Settings
    is_public = Column(Boolean, default=False)
    metadata = Column(JSON, default=dict)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())
