"""AI 服务入口（FastAPI）

能力：
1. /ai/chat           买家 AI 客服（LangChain Agent + Tool Calling + RAG）
2. /ai/review-reply   卖家 AI 生成评价回复
3. /ai/business-analysis 卖家 AI 经营分析
会话历史缓存在内存中（演示用，生产建议换 Redis）。
"""
import threading
from typing import Optional

from fastapi import FastAPI, HTTPException
from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import BaseModel

from app import agent, config

app = FastAPI(title="Mall AI Service", version="1.0.0")


class ChatRequest(BaseModel):
    userId: int
    sessionId: str
    message: str


class ChatResponse(BaseModel):
    reply: str


class ReviewReplyRequest(BaseModel):
    """卖家 AI 回复评价请求"""
    productName: str
    reviewContent: str
    rating: Optional[int] = None
    sellerReply: Optional[str] = None  # 已有回复时用于优化润色


class BusinessAnalysisRequest(BaseModel):
    """卖家 AI 经营分析请求"""
    stats: dict  # 后端统计结果：productCount/stockTotal/salesTotal/gmv/orderCount/pendingShip/trend


# 会话缓存: sessionId -> {user_id, agent, history}
_sessions: dict[str, dict] = {}
_lock = threading.Lock()


def _get_session(user_id: int, session_id: str) -> dict:
    with _lock:
        session = _sessions.get(session_id)
        if session is None or session["user_id"] != user_id:
            session = {"user_id": user_id, "agent": agent.ChatAgent(user_id), "history": []}
            _sessions[session_id] = session
        return session


def _get_llm():
    if not config.DEEPSEEK_API_KEY:
        raise RuntimeError("未配置 DEEPSEEK_API_KEY，请在 ai-service/.env 中填写")
    return agent._get_llm()


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/ai/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    session = _get_session(req.userId, req.sessionId)
    history = session["history"]

    # 追加用户消息
    history.append({"role": "user", "content": req.message})

    try:
        reply = session["agent"].chat(history, req.message)
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        reply = f"抱歉，AI 服务处理出错了：{e}"

    # 追加助手回复（失败时不追加，便于用户重试）
    if not reply.startswith("抱歉，AI 服务处理出错了"):
        history.append({"role": "assistant", "content": reply})
        # 控制内存增长
        if len(history) > 50:
            session["history"] = history[-40:]
    return ChatResponse(reply=reply)


@app.post("/ai/review-reply", response_model=ChatResponse)
def review_reply(req: ReviewReplyRequest):
    """卖家 AI 生成评价回复：根据商品名、评价内容与评分生成得体、有温度的中文回复"""
    prompt = (
        f"你是『{req.productName}』店铺的客服。请针对以下买家评价生成一段【80字以内、礼貌诚恳、有温度】的回复，"
        f"不要编造订单或物流信息，结尾可用问候语。\n"
        f"买家评分：{req.rating if req.rating is not None else '未评分'}\n"
        f"买家评价：{req.reviewContent}"
    )
    try:
        resp = _get_llm().invoke(
            [SystemMessage(content="你是电商店铺客服，回复要求：真诚、简洁、口语化、不使用 AI 味过重的套话。"),
             HumanMessage(content=prompt)]
        )
        reply = str(resp.content).strip()
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 生成失败：{e}")
    return ChatResponse(reply=reply)


@app.post("/ai/business-analysis", response_model=ChatResponse)
def business_analysis(req: BusinessAnalysisRequest):
    """卖家 AI 经营分析：基于统计数据生成经营洞察与建议（300字内）"""
    import json as _json

    prompt = (
        "你是电商资深运营专家。请基于以下店铺经营数据生成【300字以内】的经营分析报告，"
        "包括：经营概况、趋势解读、问题发现、下一步优化建议。数据说话，逻辑清晰。\n\n"
        f"经营数据：{_json.dumps(req.stats, ensure_ascii=False)}"
    )
    try:
        resp = _get_llm().invoke(
            [SystemMessage(content="你是资深电商运营专家，输出分析报告时用结构化小标题，简洁专业。"),
             HumanMessage(content=prompt)]
        )
        reply = str(resp.content).strip()
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 分析失败：{e}")
    return ChatResponse(reply=reply)
