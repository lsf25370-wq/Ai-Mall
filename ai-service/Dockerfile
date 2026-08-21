# =====================================================
# ai-service 镜像（FastAPI + LangChain）
# =====================================================
FROM python:3.12-slim

WORKDIR /app

# 时区
ENV TZ=Asia/Shanghai

# 先装依赖，利用层缓存
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 拷贝应用代码
COPY app ./app
COPY .env .env

EXPOSE 8001

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
