# =====================================================
# 一键部署脚本（Windows PowerShell 版）
# 用法：在项目根目录执行  .\deploy.ps1
# =====================================================
$ErrorActionPreference = "Stop"

Write-Host "==> [1/3] 检查 Docker 环境"
docker --version | Out-Null
if ($LASTEXITCODE -ne 0) { Write-Host "请先安装 Docker"; exit 1 }

Write-Host "==> [2/3] 构建并启动全部服务（首次构建需下载依赖，较慢）"
docker compose up -d --build
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "==> [3/3] 等待服务就绪"
Start-Sleep -Seconds 5
docker compose ps

Write-Host ""
Write-Host "==================== 部署完成 ===================="
Write-Host "买家商城   http://localhost:5173"
Write-Host "平台后台   http://localhost:5175"
Write-Host "卖家中心   http://localhost:5176"
Write-Host "后端 API   http://localhost:8080"
Write-Host "AI 服务    http://localhost:8001/health"
Write-Host "=================================================="
