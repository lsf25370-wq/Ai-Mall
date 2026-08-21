"""AI 客服工具：通过内部接口调用商城主后端，获取真实业务数据

安全说明：所有工具都只允许查询当前用户(user_id)名下数据，
主后端 InternalAiController 也会二次校验归属，防止越权。
"""
import httpx

from app import config

HEADERS = {"X-Internal-Key": config.MALL_INTERNAL_KEY}


def _get(path: str, params: dict) -> dict:
    with httpx.Client(timeout=10) as client:
        resp = client.get(
            f"{config.MALL_INTERNAL_URL}{path}",
            params=params,
            headers=HEADERS,
        )
        resp.raise_for_status()
        return resp.json()


def query_order(order_no: str, user_id: int) -> dict:
    """按订单号查询指定用户的订单状态、物流和商品明细"""
    return _get("/internal/ai/order", {"orderNo": order_no, "userId": user_id})


def query_logistics(order_no: str, user_id: int) -> dict:
    """按订单号查询指定用户的订单物流轨迹"""
    return _get("/internal/ai/logistics", {"orderNo": order_no, "userId": user_id})


def list_orders(user_id: int) -> dict:
    """查询指定用户的全部订单列表"""
    return _get("/internal/ai/orders", {"userId": user_id})


def search_products(keyword: str = "", max_price: float = None) -> dict:
    """按关键词/价格上限搜索商城在售商品"""
    params = {"keyword": keyword}
    if max_price is not None:
        params["maxPrice"] = max_price
    data = _get("/internal/ai/products", params)
    return {"products": data}


def recommend_products(keyword: str = "", max_price: float = None, category_id: int = None) -> dict:
    """按关键词、价格上限、分类推荐商城热销商品"""
    params = {"keyword": keyword}
    if max_price is not None:
        params["maxPrice"] = max_price
    if category_id is not None:
        params["categoryId"] = category_id
    data = _get("/internal/ai/recommend", params)
    return {"products": data}


def list_faq() -> list:
    """拉取 FAQ 知识库文档（RAG 检索数据源）"""
    return _get("/internal/ai/faq", {})
