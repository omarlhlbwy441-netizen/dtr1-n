import chromadb
from chromadb.config import Settings
from typing import List, Dict, Any, Optional
import hashlib
from app.core.config import settings

class RAGService:
    def __init__(self):
        self.client = None
        self._initialized = False

    def initialize(self):
        if not self._initialized:
            self.client = chromadb.HttpClient(
                host=settings.CHROMA_URL.replace("http://", "").replace(":8000", ""),
                port=8000,
                settings=Settings(allow_reset=True, anonymized_telemetry=False)
            )
            self._initialized = True

    def _get_collection_name(self, kb_id: int) -> str:
        return f"kb_{kb_id}_{hashlib.md5(str(kb_id).encode()).hexdigest()[:8]}"

    def create_knowledge_base(self, kb_id: int, name: str) -> str:
        self.initialize()
        collection_name = self._get_collection_name(kb_id)

        try:
            self.client.create_collection(
                name=collection_name,
                metadata={"kb_name": name, "kb_id": kb_id}
            )
        except Exception:
            pass  # Already exists

        return collection_name

    def add_documents(self, kb_id: int, texts: List[str], metadatas: List[Dict] = None) -> int:
        self.initialize()
        collection_name = self._get_collection_name(kb_id)

        try:
            collection = self.client.get_collection(name=collection_name)
        except Exception:
            collection = self.client.create_collection(name=collection_name)

        ids = [f"doc_{kb_id}_{i}" for i in range(len(texts))]

        collection.add(
            documents=texts,
            ids=ids,
            metadatas=metadatas or [{} for _ in texts]
        )

        return len(texts)

    def query(self, kb_id: int, query_text: str, top_k: int = 5) -> List[Dict[str, Any]]:
        self.initialize()
        collection_name = self._get_collection_name(kb_id)

        try:
            collection = self.client.get_collection(name=collection_name)
        except Exception:
            return []

        results = collection.query(
            query_texts=[query_text],
            n_results=top_k
        )

        documents = []
        for i in range(len(results["documents"][0])):
            documents.append({
                "content": results["documents"][0][i],
                "metadata": results["metadatas"][0][i] if results["metadatas"] else {},
                "distance": results["distances"][0][i] if results["distances"] else 0,
            })

        return documents

    def delete_knowledge_base(self, kb_id: int) -> bool:
        self.initialize()
        collection_name = self._get_collection_name(kb_id)

        try:
            self.client.delete_collection(name=collection_name)
            return True
        except Exception:
            return False

rag_service = RAGService()
