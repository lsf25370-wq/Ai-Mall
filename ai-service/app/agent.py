"""基于 LangChain 的 AI 客服 Agent

核心能力：
1. Tool Calling：识别用户意图，调用订单查询 / 订单列表 / 物流查询 / 商品搜索 / 商品推荐工具获取真实数据
2. RAG 检索：售后政策类问题先检索 FAQ 知识库，再结合检索结果作答，避免编造
3. 多轮对话：结合会话历史回答，避免重复询问
4. 安全约束：所有订单查询只针对当前登录用户(user_id)，杜绝越权

仅支持具备 Function Calling 能力的模型（DeepSeek V3 等）。
"""
import json
from functools import lru_cache

from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.tools import StructuredTool
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field

from app import config, rag, tools

SYSTEM_PROMPT = """你是"AI 商城"的智能客服助手，负责帮助用户解决订单、物流、售后和商品相关的问题。

工作规则：
1. 当用户查询订单状态时，必须调用 query_order 工具获取【真实数据】，严禁编造订单信息。
2. 当用户询问"我的订单""有哪些订单"时，调用 list_orders 工具。
3. 当用户询问物流、包裹到哪了时，调用 query_logistics 工具。
4. 当用户询问商品推荐、有什么手机卖时，调用 search_products 或 recommend_products 工具。
5. 当用户询问售后政策（退换货、发票、配送时效、会员权益等）时，必须先调用 retrieve_faq 检索知识库，依据检索到的政策作答；若检索不到相关内容，如实说明。
6. 订单查询需要订单号：如果用户没有提供订单号，请礼貌地请用户提供订单号（格式如 ORD202608180001）。
7. 回答订单问题时，用简洁清晰的中文说明订单状态、商品和物流信息。
8. 商品推荐时，可结合商品销量、价格给出 2-3 个推荐并说明理由。
9. 如问题不在你的能力范围内（如聊天、无关话题），礼貌说明你是商城客服，只负责订单/物流/售后/商品问题。"""


class QueryOrderInput(BaseModel):
    """查询订单参数"""
    order_no: str = Field(description="订单号，格式如 ORD202608180001")


class ListOrdersInput(BaseModel):
    """查询订单列表参数（无参数）"""


class SearchProductsInput(BaseModel):
    """搜索商品参数"""
    keyword: str = Field(default="", description="商品搜索关键词，可为空表示热销商品")
    max_price: float = Field(default=None, description="价格上限（元），可选")


class RecommendProductsInput(BaseModel):
    """推荐商品参数"""
    keyword: str = Field(default="", description="商品关键词，如手机、耳机、咖啡，可为空")
    max_price: float = Field(default=None, description="预算上限（元），可选")
    category_id: int = Field(default=None, description="商品分类ID，可选")


class RetrieveFaqInput(BaseModel):
    """检索 FAQ 知识库参数"""
    query: str = Field(description="用户想咨询的问题描述")


def _build_tools(user_id: int):
    def query_order(order_no: str) -> str:
        """查询订单状态和物流"""
        try:
            data = tools.query_order(order_no=order_no, user_id=user_id)
            return json.dumps(data, ensure_ascii=False)
        except Exception as e:
            return f"查询订单失败：{e}"

    def query_logistics(order_no: str) -> str:
        """查询订单物流轨迹"""
        try:
            data = tools.query_logistics(order_no=order_no, user_id=user_id)
            return json.dumps(data, ensure_ascii=False)
        except Exception as e:
            return f"查询物流失败：{e}"

    def list_orders() -> str:
        """查询用户全部订单"""
        try:
            data = tools.list_orders(user_id=user_id)
            return json.dumps(data, ensure_ascii=False)
        except Exception as e:
            return f"查询订单列表失败：{e}"

    def search_products(keyword: str = "", max_price: float = None) -> str:
        """搜索商城在售商品"""
        try:
            data = tools.search_products(keyword=keyword, max_price=max_price)
            return json.dumps(data, ensure_ascii=False)
        except Exception as e:
            return f"搜索商品失败：{e}"

    def recommend_products(keyword: str = "", max_price: float = None, category_id: int = None) -> str:
        """推荐商城热销商品"""
        try:
            data = tools.recommend_products(keyword=keyword, max_price=max_price, category_id=category_id)
            return json.dumps(data, ensure_ascii=False)
        except Exception as e:
            return f"推荐商品失败：{e}"

    def retrieve_faq(query: str) -> str:
        """检索 FAQ 知识库中的售后政策"""
        context = rag.format_context(query)
        return context if context else "知识库中未检索到相关内容，请告知用户直接咨询人工客服。"

    return [
        StructuredTool.from_function(
            func=query_order,
            name="query_order",
            description="按订单号查询当前用户的订单状态、物流信息和商品明细。用户询问订单到哪了/发货了吗时使用。",
            args_schema=QueryOrderInput,
        ),
        StructuredTool.from_function(
            func=query_logistics,
            name="query_logistics",
            description="按订单号查询当前用户的订单物流轨迹（揽收/运输/派送节点）。用户询问物流到哪了/包裹到哪时使用。",
            args_schema=QueryOrderInput,
        ),
        StructuredTool.from_function(
            func=list_orders,
            name="list_orders",
            description="查询当前用户的全部订单列表（订单号、状态、金额）。用户询问『我的订单』『我有哪些订单』时使用。",
            args_schema=ListOrdersInput,
        ),
        StructuredTool.from_function(
            func=search_products,
            name="search_products",
            description="按关键词搜索商城在售商品，返回商品名称、价格、销量。用户询问有什么商品卖时使用。",
            args_schema=SearchProductsInput,
        ),
        StructuredTool.from_function(
            func=recommend_products,
            name="recommend_products",
            description="结合关键词、预算上限、分类推荐商城热销商品。用户说『预算3000以内推荐手机』等带预算/推荐意图时使用。",
            args_schema=RecommendProductsInput,
        ),
        StructuredTool.from_function(
            func=retrieve_faq,
            name="retrieve_faq",
            description="检索商城 FAQ 知识库中的售后政策（退换货、发票、配送时效、会员权益等）。用户询问售后政策时必须先调用本工具。",
            args_schema=RetrieveFaqInput,
        ),
    ]


@lru_cache(maxsize=1)
def _get_llm() -> ChatOpenAI:
    if not config.DEEPSEEK_API_KEY:
        raise RuntimeError("未配置 DEEPSEEK_API_KEY，请在 ai-service/.env 中填写")
    return ChatOpenAI(
        model=config.DEEPSEEK_MODEL,
        api_key=config.DEEPSEEK_API_KEY,
        base_url=config.DEEPSEEK_BASE_URL,
        temperature=0.3,
        timeout=60,
    )


class ChatAgent:
    """单个用户的客服 Agent（绑定 user_id，保证数据隔离）"""

    def __init__(self, user_id: int):
        self.user_id = user_id
        self.tools = _build_tools(user_id)
        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", SYSTEM_PROMPT),
                MessagesPlaceholder("chat_history"),
                ("human", "{input}"),
                MessagesPlaceholder("agent_scratchpad"),
            ]
        )
        agent = create_tool_calling_agent(_get_llm(), self.tools, prompt)
        self.executor = AgentExecutor(
            agent=agent,
            tools=self.tools,
            max_iterations=4,
            verbose=False,
        )

    def chat(self, history: list[dict], message: str) -> str:
        """history: [{role: user/assistant, content: str}, ...]"""
        messages = []
        # RAG：将相关 FAQ 知识注入系统消息，作为售后政策的权威依据
        faq_context = rag.format_context(message)
        if faq_context:
            messages.append(SystemMessage(content=faq_context))
        for item in history[-config.MAX_HISTORY:]:
            if item["role"] == "user":
                messages.append(HumanMessage(content=item["content"]))
            else:
                messages.append(AIMessage(content=item["content"]))
        result = self.executor.invoke({"input": message, "chat_history": messages})
        return str(result.get("output", "")).strip()
