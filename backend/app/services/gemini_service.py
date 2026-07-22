import asyncio
import time
import google.generativeai as genai
from typing import Optional, List, Dict, Any, AsyncGenerator
from app.core.config import settings

class GeminiService:
    def __init__(self):
        self.configured = False
        self.model = None
        if settings.GEMINI_API_KEY:
            genai.configure(api_key=settings.GEMINI_API_KEY)
            self.model = genai.GenerativeModel(settings.GEMINI_MODEL)
            self.configured = True

    def _build_system_prompt(self, base_prompt: str, kb_context: str = "", expert_mode: str = "general") -> str:
        """Build a rich system prompt with context"""
        modes = {
            "general": "You are Wolf AI, a helpful and knowledgeable assistant.",
            "coding": "You are Wolf AI, an expert programmer. Write clean, efficient, well-documented code.",
            "analysis": "You are Wolf AI, a data analyst. Provide deep insights with evidence.",
            "writing": "You are Wolf AI, a professional writer. Create engaging, well-structured content.",
            "legal": "You are Wolf AI, a legal research assistant. Provide accurate legal information with citations. Note: This is not legal advice.",
            "medical": "You are Wolf AI, a medical research assistant. Provide evidence-based information. Note: This is not medical advice.",
            "research": "You are Wolf AI, a research scientist. Provide well-sourced, evidence-based answers.",
            "creative": "You are Wolf AI, a creative partner. Think outside the box and provide innovative ideas.",
            "math": "You are Wolf AI, a mathematics professor. Show step-by-step solutions.",
            "translation": "You are Wolf AI, a professional translator. Maintain nuance and cultural context.",
        }

        system = modes.get(expert_mode, modes["general"])

        if base_prompt and base_prompt != "You are Wolf AI, a helpful internal assistant.":
            system = base_prompt

        if kb_context:
            system += f"

Use the following knowledge base context to answer:
{kb_context}"

        system += "

Respond in the same language as the user's query."
        return system

    async def chat(
        self,
        message: str,
        conversation_history: Optional[List[Dict]] = None,
        system_prompt: str = "",
        kb_context: str = "",
        expert_mode: str = "general",
        temperature: float = 0.7,
        max_tokens: int = 2048
    ) -> Dict[str, Any]:
        if not self.configured:
            raise ValueError("Gemini API not configured")

        system = self._build_system_prompt(system_prompt, kb_context, expert_mode)

        model = genai.GenerativeModel(
            model_name=settings.GEMINI_MODEL,
            system_instruction=system,
            generation_config={
                "temperature": temperature,
                "max_output_tokens": max_tokens,
            }
        )

        chat = model.start_chat(history=[])

        if conversation_history:
            for msg in conversation_history:
                if msg["role"] == "user":
                    chat.history.append({"role": "user", "parts": [msg["content"]]})
                elif msg["role"] == "assistant":
                    chat.history.append({"role": "model", "parts": [msg["content"]]})

        start_time = time.time()
        response = await asyncio.to_thread(chat.send_message, message)
        latency = (time.time() - start_time) * 1000

        return {
            "content": response.text,
            "tokens_used": response.usage_metadata.total_token_count if hasattr(response, "usage_metadata") else 0,
            "latency_ms": latency,
            "model_version": settings.GEMINI_MODEL,
        }

    async def chat_stream(
        self,
        message: str,
        conversation_history: Optional[List[Dict]] = None,
        system_prompt: str = "",
        kb_context: str = "",
        expert_mode: str = "general",
    ) -> AsyncGenerator[str, None]:
        if not self.configured:
            yield "Error: Gemini API not configured"
            return

        system = self._build_system_prompt(system_prompt, kb_context, expert_mode)

        model = genai.GenerativeModel(
            model_name=settings.GEMINI_MODEL,
            system_instruction=system
        )

        chat = model.start_chat(history=[])
        if conversation_history:
            for msg in conversation_history:
                if msg["role"] == "user":
                    chat.history.append({"role": "user", "parts": [msg["content"]]})
                elif msg["role"] == "assistant":
                    chat.history.append({"role": "model", "parts": [msg["content"]]})

        response = await asyncio.to_thread(chat.send_message, message, stream=True)

        for chunk in response:
            if chunk.text:
                yield chunk.text

    async def analyze_document(self, text: str, query: str) -> Dict[str, Any]:
        """Analyze a document and answer questions about it"""
        prompt = f"""Document content:
{text[:30000]}

Based on the document above, answer the following question:
{query}

If the answer is not in the document, say "I cannot find this information in the document.""""

        return await self.chat(prompt, expert_mode="analysis")

    async def summarize(self, text: str, max_length: int = 500) -> Dict[str, Any]:
        """Summarize text"""
        prompt = f"Summarize the following text in {max_length} words or less:

{text[:30000]}"
        return await self.chat(prompt, expert_mode="general")

    async def generate_code(self, description: str, language: str = "python") -> Dict[str, Any]:
        """Generate code"""
        prompt = f"Write {language} code for: {description}

Include comments and error handling."
        return await self.chat(prompt, expert_mode="coding")

    async def translate(self, text: str, target_language: str, source_language: str = "auto") -> Dict[str, Any]:
        """Translate text"""
        prompt = f"Translate the following text from {source_language} to {target_language}:

{text}"
        return await self.chat(prompt, expert_mode="translation")

gemini_service = GeminiService()
