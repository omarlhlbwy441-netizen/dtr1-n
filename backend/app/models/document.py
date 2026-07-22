from sqlalchemy import Column, Integer, String, Text, DateTime, BigInteger, JSON, ForeignKey
from sqlalchemy.sql import func
from app.core.database import Base

class Document(Base):
    __tablename__ = "documents"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)

    filename = Column(String(255), nullable=False)
    original_name = Column(String(255), nullable=False)
    mime_type = Column(String(100), nullable=False)
    size_bytes = Column(BigInteger, default=0)

    # Content
    extracted_text = Column(Text)
    content_summary = Column(Text)

    # ChromaDB
    chroma_collection = Column(String(100))
    chunk_count = Column(Integer, default=0)

    # Metadata
    metadata = Column(JSON, default=dict)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
