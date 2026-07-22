from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from sqlalchemy.ext.asyncio import AsyncSession
from typing import Optional
from pydantic import BaseModel

from app.core.database import get_db
from app.core.config import settings
from app.services.ai_factory import AIFactory
from app.services.rag_service import rag_service

router = APIRouter(prefix="/chat", tags=["Chat"])

class ChatRequest(BaseModel):
    message: str
    conversation_id: Optional[int] = None
    expert_mode: str = "general"
    use_rag: bool = False
    temperature: float = 0.7
    max_tokens: int = 2048

class ChatResponse(BaseModel):
    id: int
    content: str
    conversation_id: int
    tokens_used: int
    latency_ms: float
    model_version: str
    sources: list = []

# In-memory storage for demo (replace with DB)
conversations_db = {}
messages_db = {}
message_counter = 0

@router.post("/", response_model=ChatResponse)
async def chat(request: ChatRequest, db: AsyncSession = Depends(get_db)):
    """Send a message to the AI and get a response"""

    try:
        # Get AI service
        ai_service = AIFactory.get_service()

        # Get conversation history
        conversation_history = []
        if request.conversation_id and request.conversation_id in messages_db:
            conversation_history = messages_db[request.conversation_id]

        # Get knowledge base context if RAG is enabled
        kb_context = ""
        if request.use_rag:
            try:
                kb_results = await rag_service.search(request.message, top_k=settings.RAG_TOP_K)
                if kb_results:
                    kb_context = "\n".join([r.get("content", "") for r in kb_results])
            except Exception as e:
                kb_context = ""

        # Call AI
        result = await ai_service.chat(
            message=request.message,
            conversation_history=conversation_history,
            kb_context=kb_context,
            expert_mode=request.expert_mode,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
        )

        # Create or get conversation
        global message_counter
        message_counter += 1
        conv_id = request.conversation_id or message_counter

        if conv_id not in conversations_db:
            conversations_db[conv_id] = {
                "id": conv_id,
                "title": request.message[:50] + "..." if len(request.message) > 50 else request.message,
                "expert_mode": request.expert_mode,
            }

        if conv_id not in messages_db:
            messages_db[conv_id] = []

        # Store messages
        messages_db[conv_id].append({"role": "user", "content": request.message})
        messages_db[conv_id].append({"role": "assistant", "content": result["content"]})

        return ChatResponse(
            id=message_counter,
            content=result["content"],
            conversation_id=conv_id,
            tokens_used=result.get("tokens_used", 0),
            latency_ms=result.get("latency_ms", 0),
            model_version=result.get("model_version", "unknown"),
            sources=result.get("sources", []),
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI Error: {str(e)}")

@router.get("/conversations")
async def get_conversations():
    """Get all conversations"""
    return list(conversations_db.values())

@router.get("/conversations/{conv_id}/messages")
async def get_messages(conv_id: int):
    """Get messages for a conversation"""
    return messages_db.get(conv_id, [])

@router.post("/conversations")
async def create_conversation():
    """Create a new conversation"""
    global message_counter
    message_counter += 1
    conv_id = message_counter
    conversations_db[conv_id] = {
        "id": conv_id,
        "title": "New Chat",
        "expert_mode": "general",
    }
    messages_db[conv_id] = []
    return conversations_db[conv_id]

@router.delete("/conversations/{conv_id}")
async def delete_conversation(conv_id: int):
    """Delete a conversation"""
    if conv_id in conversations_db:
        del conversations_db[conv_id]
    if conv_id in messages_db:
        del messages_db[conv_id]
    return {"status": "deleted"}

@router.get("/health")
async def health_check():
    """Check AI health"""
    return await AIFactory.health_check()
