from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.user import User
from app.models.knowledge_base import KnowledgeBase
from app.services.rag_service import rag_service

router = APIRouter()

@router.post("/")
async def create_kb(data: dict, current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    kb = KnowledgeBase(
        user_id=current_user.id,
        name=data.get("name"),
        description=data.get("description", ""),
        collection_name=f"kb_{current_user.id}_{data.get('name').replace(' ', '_').lower()}"
    )
    db.add(kb)
    await db.commit()
    await db.refresh(kb)

    # Create Chroma collection
    rag_service.create_knowledge_base(kb.id, kb.name)

    return {"id": kb.id, "name": kb.name, "collection": kb.collection_name}

@router.get("/")
async def list_kbs(current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(KnowledgeBase).where(KnowledgeBase.user_id == current_user.id))
    kbs = result.scalars().all()
    return [{"id": k.id, "name": k.name, "description": k.description, "documents": k.document_count, "created_at": k.created_at} for k in kbs]

@router.delete("/{kb_id}")
async def delete_kb(kb_id: int, current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(KnowledgeBase).where(KnowledgeBase.id == kb_id, KnowledgeBase.user_id == current_user.id))
    kb = result.scalar_one_or_none()
    if not kb:
        raise HTTPException(status_code=404, detail="Not found")

    rag_service.delete_knowledge_base(kb_id)
    await db.delete(kb)
    await db.commit()

    return {"success": True}
