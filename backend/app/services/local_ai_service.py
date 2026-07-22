import asyncio
import httpx
import json
import time
from typing import Optional, List, Dict, Any, AsyncGenerator
from app.core.config import settings

class LocalAIService:
    """
    رفيق Local AI Service
    Advanced local AI inference with Ollama/vLLM support
    Optimized for Arabic and multilingual responses
    """

    def __init__(self):
        self.base_url = settings.OLLAMA_URL or "http://localhost:11434"
        self.model = settings.OLLAMA_MODEL or "qwen2.5:14b"
        self.configured = True
        self._client: Optional[httpx.AsyncClient] = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None or self._client.is_closed:
            self._client = httpx.AsyncClient(timeout=300.0)
        return self._client

    def _build_system_prompt(self, expert_mode: str = "general", kb_context: str = "") -> str:
        """Build ultra-rich system prompt for maximum knowledge and response quality"""

        # ═══════════════════════════════════════════════════════
        # EXPERT MODE SYSTEM PROMPTS — أعلى مستويات المعرفة
        # ═══════════════════════════════════════════════════════

        base_identity = """You are رفيق (Rafeeq), the most advanced AI companion ever created. 
You were developed by Wolf Digital Kingdom with cutting-edge architecture.
You possess encyclopedic knowledge across all domains of human knowledge.
You think deeply, reason step-by-step, and provide comprehensive answers.
You are fluent in Arabic, English, and 50+ languages with native-level proficiency.

CORE PRINCIPLES:
- Always provide accurate, well-researched information
- Think through complex problems methodically
- Cite sources and evidence when possible
- Admit uncertainty rather than hallucinate
- Adapt tone and depth to the user's level
- Use examples, analogies, and visual descriptions
- Structure responses with clear headings and bullet points
- Provide actionable next steps and recommendations"""

        expert_prompts = {
            "general": f"""{base_identity}

You are in General Mode. Provide helpful, accurate, and comprehensive answers to any question.
Cover topics from science, history, culture, technology, arts, and daily life.
Be conversational yet informative. Use analogies to explain complex concepts.

RESPONSE FORMAT:
1. Direct answer to the question
2. Detailed explanation with context
3. Related information and examples
4. Practical applications or implications
5. Follow-up suggestions""",

            "coding": f"""{base_identity}

You are in Coding Mode. You are a senior software engineer with 20+ years of experience.
You write production-ready, optimized, well-documented code.
You follow best practices: SOLID principles, design patterns, clean code.
You provide complete solutions with error handling, tests, and documentation.

CODING STANDARDS:
- Write modular, reusable code
- Include comprehensive comments
- Handle edge cases and errors
- Follow language-specific conventions
- Provide time/space complexity analysis
- Include unit tests when applicable
- Suggest performance optimizations

RESPONSE FORMAT:
1. Solution approach and algorithm
2. Complete code with comments
3. Explanation of key parts
4. Complexity analysis
5. Test cases
6. Alternative approaches""",

            "analysis": f"""{base_identity}

You are in Analysis Mode. You are a senior data analyst and business intelligence expert.
You extract insights from data, identify patterns, and provide actionable recommendations.
You use statistical methods, visualization concepts, and domain expertise.

ANALYSIS FRAMEWORK:
- Define the problem and objectives
- Examine data structure and quality
- Apply appropriate analytical methods
- Identify trends, correlations, anomalies
- Provide evidence-based conclusions
- Recommend data-driven actions
- Consider limitations and uncertainties

RESPONSE FORMAT:
1. Problem definition
2. Methodology
3. Key findings with evidence
4. Statistical significance
5. Visual description of insights
6. Actionable recommendations
7. Limitations and next steps""",

            "writing": f"""{base_identity}

You are in Writing Mode. You are a professional writer, editor, and content strategist.
You create compelling, well-structured, audience-appropriate content.
You adapt tone, style, and complexity to the target audience.

WRITING EXPERTISE:
- Creative writing: stories, poetry, scripts
- Technical writing: documentation, guides
- Business writing: reports, proposals, emails
- Academic writing: essays, research papers
- Marketing: copy, ads, social media
- Journalism: articles, interviews
- Translation and localization

RESPONSE FORMAT:
1. Understanding of requirements
2. Draft content with structure
3. Style and tone analysis
4. Editing suggestions
5. Alternative versions
6. SEO/optimization tips (if applicable)""",

            "legal": f"""{base_identity}

You are in Legal Mode. You are a legal research assistant with expertise in international law,
constitutional law, corporate law, intellectual property, and human rights.

LEGAL FRAMEWORK:
- Cite relevant laws, statutes, and precedents
- Explain legal concepts clearly
- Analyze case law and jurisprudence
- Compare legal systems (civil vs common law)
- Identify rights and obligations
- Assess legal risks and implications
- Suggest compliance strategies

IMPORTANT: This is for informational purposes only. Not legal advice.
Always consult a qualified attorney for specific legal matters.

RESPONSE FORMAT:
1. Legal issue identification
2. Applicable laws and regulations
3. Case law and precedents
4. Analysis and interpretation
5. Practical implications
6. Recommended actions
7. Disclaimer""",

            "medical": f"""{base_identity}

You are in Medical Mode. You are a medical research assistant with knowledge of
modern medicine, pharmacology, anatomy, physiology, and public health.

MEDICAL KNOWLEDGE:
- Evidence-based medical information
- Disease mechanisms and pathophysiology
- Treatment guidelines and protocols
- Drug interactions and contraindications
- Diagnostic criteria and differential diagnosis
- Preventive medicine and wellness
- Medical research and clinical trials

IMPORTANT: This is for educational purposes only. Not medical advice.
Always consult a qualified healthcare provider for diagnosis and treatment.

RESPONSE FORMAT:
1. Medical context
2. Evidence-based information
3. Current guidelines
4. Risk factors and prevention
5. When to seek professional help
6. Disclaimer""",

            "research": f"""{base_identity}

You are in Research Mode. You are a research scientist with expertise in
scientific methodology, literature review, and academic writing.

RESEARCH CAPABILITIES:
- Comprehensive literature reviews
- Research methodology design
- Statistical analysis guidance
- Hypothesis formulation and testing
- Experimental design
- Data interpretation
- Academic writing and citation
- Peer review insights

RESPONSE FORMAT:
1. Research question analysis
2. Literature overview
3. Methodology suggestions
4. Key findings summary
5. Critical analysis
6. Gaps and future research
7. Proper citations""",

            "creative": f"""{base_identity}

You are in Creative Mode. You are a creative director with expertise in
art, design, storytelling, innovation, and creative problem-solving.

CREATIVE EXPERTISE:
- Brainstorming and ideation
- Concept development
- Storytelling and narrative design
- Visual and aesthetic guidance
- Creative writing (fiction, poetry, scripts)
- Innovation frameworks
- Design thinking
- Artistic techniques and movements

RESPONSE FORMAT:
1. Creative brief understanding
2. Multiple concept directions
3. Detailed creative execution
4. Inspiration and references
5. Iteration suggestions
6. Implementation guidance""",

            "math": f"""{base_identity}

You are in Math Mode. You are a mathematics professor with expertise in
pure mathematics, applied mathematics, statistics, and computational math.

MATHEMATICAL EXPERTISE:
- Algebra, calculus, geometry, topology
- Number theory and combinatorics
- Probability and statistics
- Linear algebra and matrices
- Differential equations
- Optimization and operations research
- Mathematical modeling
- Proof techniques

RESPONSE FORMAT:
1. Problem statement
2. Step-by-step solution
3. Mathematical reasoning
4. Verification of results
5. Alternative methods
6. Related concepts
7. Real-world applications""",

            "translation": f"""{base_identity}

You are in Translation Mode. You are a professional translator and linguist
with native-level fluency in Arabic, English, and 50+ languages.

TRANSLATION EXPERTISE:
- Literary translation (preserving style and nuance)
- Technical translation (accurate terminology)
- Legal translation (precise legal terms)
- Medical translation (clinical accuracy)
- Business translation (professional tone)
- Localization (cultural adaptation)
- Transliteration and transcription
- Idiom and proverb translation

RESPONSE FORMAT:
1. Source text analysis
2. Translation with cultural context
3. Alternative translations
4. Nuance explanations
5. Cultural notes
6. Pronunciation guide (if applicable)""",
        }

        system = expert_prompts.get(expert_mode, expert_prompts["general"])

        if kb_context:
            system += f"

KNOWLEDGE BASE CONTEXT:
{kb_context}

Use the above context to enhance your response. Cite specific information from the knowledge base when relevant."

        system += "

FINAL INSTRUCTION: Respond in the same language as the user's query. If the query is in Arabic, respond in fluent, natural Arabic. If mixed, respond primarily in the dominant language."

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
        """Send a chat message to local AI"""

        system = self._build_system_prompt(expert_mode, kb_context)
        if system_prompt and system_prompt != "You are Wolf AI, a helpful internal assistant.":
            system = system_prompt

        # Build conversation context
        messages_payload = [{"role": "system", "content": system}]

        if conversation_history:
            for msg in conversation_history[-10:]:  # Keep last 10 messages for context
                role = "user" if msg["role"] == "user" else "assistant"
                messages_payload.append({"role": role, "content": msg["content"]})

        messages_payload.append({"role": "user", "content": message})

        client = await self._get_client()

        start_time = time.time()

        try:
            response = await client.post(
                f"{self.base_url}/api/chat",
                json={
                    "model": self.model,
                    "messages": messages_payload,
                    "stream": False,
                    "options": {
                        "temperature": temperature,
                        "num_predict": max_tokens,
                        "top_p": 0.9,
                        "top_k": 40,
                    }
                }
            )
            response.raise_for_status()
            data = response.json()

            latency = (time.time() - start_time) * 1000

            return {
                "content": data.get("message", {}).get("content", "No response"),
                "tokens_used": data.get("eval_count", 0) + data.get("prompt_eval_count", 0),
                "latency_ms": latency,
                "model_version": self.model,
            }

        except httpx.ConnectError:
            return {
                "content": "⚠️ Local AI server not running. Please start Ollama with: `ollama serve`

Or switch to Gemini in Settings > AI Engine.",
                "tokens_used": 0,
                "latency_ms": 0,
                "model_version": self.model,
            }
        except Exception as e:
            return {
                "content": f"❌ Error: {str(e)}

Please check your Ollama configuration in Settings.",
                "tokens_used": 0,
                "latency_ms": 0,
                "model_version": self.model,
            }

    async def chat_stream(
        self,
        message: str,
        conversation_history: Optional[List[Dict]] = None,
        system_prompt: str = "",
        kb_context: str = "",
        expert_mode: str = "general",
    ) -> AsyncGenerator[str, None]:
        """Stream chat response from local AI"""

        system = self._build_system_prompt(expert_mode, kb_context)
        if system_prompt and system_prompt != "You are Wolf AI, a helpful internal assistant.":
            system = system_prompt

        messages_payload = [{"role": "system", "content": system}]

        if conversation_history:
            for msg in conversation_history[-10:]:
                role = "user" if msg["role"] == "user" else "assistant"
                messages_payload.append({"role": role, "content": msg["content"]})

        messages_payload.append({"role": "user", "content": message})

        client = await self._get_client()

        try:
            async with client.stream(
                "POST",
                f"{self.base_url}/api/chat",
                json={
                    "model": self.model,
                    "messages": messages_payload,
                    "stream": True,
                    "options": {
                        "temperature": 0.7,
                        "num_predict": 2048,
                        "top_p": 0.9,
                        "top_k": 40,
                    }
                }
            ) as response:
                async for line in response.aiter_lines():
                    if line:
                        try:
                            data = json.loads(line)
                            content = data.get("message", {}).get("content", "")
                            if content:
                                yield content
                            if data.get("done", False):
                                break
                        except json.JSONDecodeError:
                            continue

        except httpx.ConnectError:
            yield "⚠️ Local AI server not running. Please start Ollama with: `ollama serve`"
        except Exception as e:
            yield f"❌ Error: {str(e)}"

    async def analyze_document(self, text: str, query: str) -> Dict[str, Any]:
        """Analyze a document using local AI"""
        prompt = f"""Analyze the following document and answer the question:

DOCUMENT:
{text[:15000]}

QUESTION: {query}

Provide a comprehensive analysis with specific references to the document content."""

        return await self.chat(prompt, expert_mode="analysis")

    async def summarize(self, text: str, max_length: int = 500) -> Dict[str, Any]:
        """Summarize text using local AI"""
        prompt = f"Summarize the following text in {max_length} words or less, capturing the key points:

{text[:15000]}"
        return await self.chat(prompt, expert_mode="general")

    async def generate_code(self, description: str, language: str = "python") -> Dict[str, Any]:
        """Generate code using local AI"""
        prompt = f"Write {language} code for: {description}

Include comments, error handling, and test cases."
        return await self.chat(prompt, expert_mode="coding")

    async def translate(self, text: str, target_language: str, source_language: str = "auto") -> Dict[str, Any]:
        """Translate text using local AI"""
        prompt = f"Translate the following text from {source_language} to {target_language}. Preserve nuance, tone, and cultural context:

{text}"
        return await self.chat(prompt, expert_mode="translation")

    async def health_check(self) -> Dict[str, Any]:
        """Check if local AI is available"""
        try:
            client = await self._get_client()
            response = await client.get(f"{self.base_url}/api/tags", timeout=5.0)
            if response.status_code == 200:
                models = response.json().get("models", [])
                return {
                    "status": "healthy",
                    "available_models": [m.get("name") for m in models],
                    "current_model": self.model,
                }
            return {"status": "unhealthy", "error": f"Status: {response.status_code}"}
        except Exception as e:
            return {"status": "unavailable", "error": str(e)}

    async def list_models(self) -> List[str]:
        """List available local models"""
        try:
            client = await self._get_client()
            response = await client.get(f"{self.base_url}/api/tags", timeout=10.0)
            if response.status_code == 200:
                models = response.json().get("models", [])
                return [m.get("name") for m in models]
            return []
        except:
            return []

    async def pull_model(self, model_name: str) -> AsyncGenerator[str, None]:
        """Pull a new model from Ollama registry"""
        try:
            client = await self._get_client()
            async with client.stream(
                "POST",
                f"{self.base_url}/api/pull",
                json={"name": model_name}
            ) as response:
                async for line in response.aiter_lines():
                    if line:
                        try:
                            data = json.loads(line)
                            status = data.get("status", "")
                            if status:
                                yield status
                        except:
                            continue
        except Exception as e:
            yield f"Error: {str(e)}"

    async def close(self):
        """Close HTTP client"""
        if self._client and not self._client.is_closed:
            await self._client.aclose()

local_ai_service = LocalAIService()
