"""FAQ RAG 检索器

轻量级向量检索（无外部向量库依赖，便于本地演示）：
1. 将 FAQ 文档切分为字符 bigram 特征向量
2. 用户问题同样做 bigram 特征化
3. 用余弦相似度检索 Top-K 相关 FAQ 条目

检索结果会作为上下文注入 Agent 的 system prompt，
让 LLM 依据知识库内容回答售后类问题，避免编造政策。
"""
from functools import lru_cache

from app import tools

DEFAULT_TOP_K = 3


def _bigrams(text: str) -> dict:
    """将文本转为字符 bigram 频率向量（降噪 + 长度归一）"""
    text = "".join(ch for ch in text if ch.strip())
    tokens = [text[i : i + 2] for i in range(max(len(text) - 1, 0))]
    vec = {}
    for token in tokens:
        vec[token] = vec.get(token, 0) + 1
    norm = sum(v * v for v in vec.values()) ** 0.5 or 1.0
    return {k: v / norm for k, v in vec.items()}


def _cosine(vec_a: dict, vec_b: dict) -> float:
    if not vec_a or not vec_b:
        return 0.0
    small, large = (vec_a, vec_b) if len(vec_a) < len(vec_b) else (vec_b, vec_a)
    return sum(v * large.get(k, 0) for k, v in small.items())


@lru_cache(maxsize=1)
def _faq_docs() -> list:
    """拉取并缓存 FAQ 文档（按需更新，演示场景足够）"""
    return tools.list_faq() or []


def refresh() -> None:
    """清空 FAQ 缓存（知识库更新后调用）"""
    _faq_docs.cache_clear()


def retrieve(query: str, top_k: int = DEFAULT_TOP_K) -> list:
    """检索与 query 最相关的 Top-K 条 FAQ 文档"""
    query_vec = _bigrams(query)
    docs = _faq_docs()
    scored = []
    for doc in docs:
        title_vec = _bigrams(doc.get("title", ""))
        content_vec = _bigrams(doc.get("content", ""))
        score = 0.6 * _cosine(query_vec, title_vec) + 0.4 * _cosine(query_vec, content_vec)
        scored.append((score, doc))
    scored.sort(key=lambda x: x[0], reverse=True)
    return [doc for score, doc in scored if score > 0.05][:top_k]


def format_context(query: str, top_k: int = DEFAULT_TOP_K) -> str:
    """生成可直接注入 Prompt 的 FAQ 知识上下文"""
    hits = retrieve(query, top_k)
    if not hits:
        return ""
    lines = ["【商城 FAQ 知识库】"]
    for doc in hits:
        lines.append(f"- {doc.get('title', '')}: {doc.get('content', '')}")
    return "\n".join(lines)
