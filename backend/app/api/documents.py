from fastapi import APIRouter, Depends, UploadFile, File, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.user import User
from app.models.document import Document
from app.services.document_processor import doc_processor
from app.services.rag_service import rag_service
from app.services.gemini_service import gemini_service

router = APIRouter()

@router.post("/upload")
async def upload_document(
    file: UploadFile = File(...),
    kb_id: int = None,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    content = await file.read()

    # Extract text
    text = doc_processor.extract(content, file.content_type or "application/octet-stream")

    # Save to DB
    doc = Document(
        user_id=current_user.id,
        filename=file.filename,
        original_name=file.filename,
        mime_type=file.content_type or "application/octet-stream",
        size_bytes=len(content),
        extracted_text=text[:10000] if len(text) > 10000 else text
    )
    db.add(doc)
    await db.commit()
    await db.refresh(doc)

    # Add to RAG if kb_id provided
    if kb_id:
        chunks = doc_processor.chunk_text(text)
        rag_service.add_documents(
            kb_id=kb_id,
            texts=chunks,
            metadatas=[{"source": file.filename, "doc_id": doc.id} for _ in chunks]
        )
        doc.chunk_count = len(chunks)
        await db.commit()

    # Generate summary with Gemini
    summary = ""
    if len(text) > 100:
        try:
            result = await gemini_service.summarize(text, max_length=200)
            summary = result["content"]
            doc.content_summary = summary
            await db.commit()
        except:
            pass

    return {
        "id": doc.id,
        "filename": doc.original_name,
        "size": doc.size_bytes,
        "chunks": doc.chunk_count,
        "summary": summary
    }

@router.get("/")
async def list_documents(current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Document).where(Document.user_id == current_user.id))
    docs = result.scalars().all()
    return [{"id": d.id, "filename": d.original_name, "size": d.size_bytes, "chunks": d.chunk_count, "summary": d.content_summary, "created_at": d.created_at} for d in docs]

@router.post("/{doc_id}/analyze")
async def analyze_document(doc_id: int, data: dict, current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Document).where(Document.id == doc_id, Document.user_id == current_user.id))
    doc = result.scalar_one_or_none()
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")

    query = data.get("query", "Summarize this document")
    result = await gemini_service.analyze_document(doc.extracted_text or "", query)

    return {"analysis": result["content"], "tokens_used": result["tokens_used"]}
