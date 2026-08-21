# =====================================================
# 一键部署脚本：构建镜像 -> 启动全部服务
# 用法：在项目根目录执行  ./deploy.sh  或  bash deploy.sh
# =====================================================
#!/usr/bin/env bash
set -e

echo "==> [1/3] 检查 Docker 环境"
docker --version || { echo "请先安装 Docker"; exit 1; }

echo "==> [2/3] 构建并启动全部服务（首次构建需下载依赖，较慢）"
docker compose up -d --build

echo "==> [3/3] 等待服务就绪"
sleep 5
docker compose ps

echo ""
echo "==================== 部署完成 ===================="
echo "买家商城   http://localhost:5173"
echo "平台后台   http://localhost:5175"
echo "卖家中心   http://localhost:5176"
echo "后端 API   http://localhost:8080"
echo "AI 服务    http://localhost:8001/health"
echo "=================================================="
