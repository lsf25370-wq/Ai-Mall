"""AI 客服服务配置"""
import os

from dotenv import load_dotenv

load_dotenv()

# DeepSeek
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
DEEPSEEK_BASE_URL = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")

# 商城主后端内部接口
MALL_INTERNAL_URL = os.getenv("MALL_INTERNAL_URL", "http://localhost:8080")
MALL_INTERNAL_KEY = os.getenv("MALL_INTERNAL_KEY", "mall-internal-key-2026")

# 单次会话携带的历史消息上限（超出裁剪最早消息，控制 Token）
MAX_HISTORY = 20
