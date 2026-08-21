# AI Mall 🛍️

> 多端智能电商商城 · 模仿淘宝 / 天猫的完整电商闭环 + AI 大模型业务落地

![License](https://img.shields.io/badge/License-MIT-green)
![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-blue)
![Vue](https://img.shields.io/badge/Vue-3-green)
![LangChain](https://img.shields.io/badge/LangChain-0.3-blue)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

AI Mall 是一个**买家 / 卖家 / 管理员三端分离**的电商商城项目，覆盖商品、购物车、订单、支付、秒杀、优惠券、积分、评价等完整电商链路；同时独立部署 AI 服务，基于 **FastAPI + LangChain + DeepSeek** 实现 AI 客服、经营分析、评价智能回复等大模型能力，探索 AI 在真实电商业务中的落地。

## ✨ 功能特性

### 🛍️ 三端分离（多角色）
| 端 | 说明 | 访问端口 |
|---|---|---|
| 买家商城 | 浏览 / 搜索 / 购物车 / 下单支付 / 秒杀 / 领券 / AI 客服 | 5173 |
| 卖家中心 | 千牛风格：店铺管理 / 商品管理 / 订单发货 / 退款审批 / 数据看板 / AI 经营分析 | 5176 |
| 管理后台 | 用户管理 / 店铺管理 / 商品审核 / 订单监控 / 数据统计 | 5175 |

- JWT 三角色鉴权 + 拦截器角色校验 + 接口白名单 + 限流，防越权访问

### ⚡ 高并发实战
- **秒杀系统**：Redis 原子预扣库存 + 数据库条件扣减**双重校验防超卖**，用户限购标记，超时未支付订单自动回补库存
- **热点缓存**：Redis 缓存商品详情，接口响应大幅降低

### 🎫 营销体系
- **优惠券**：满减 / 折扣券两种类型，原子条件更新**防超发**，下单多店铺拆单精准核销
- **积分 + 会员等级**：支付得积分、积分流水、等级自动升级、升级进度展示，打通支付 → 积分 → 权益激励闭环

### 🤖 AI 应用落地
独立部署 AI 服务（Python FastAPI + LangChain），对接 DeepSeek 大模型，AI 服务不可用时自动降级：

- **AI 客服**：订单 / 物流查询、商品搜索推荐、FAQ 知识库问答（RAG）
- **AI 经营分析**：卖家经营数据分析报告生成
- **AI 评价回复**：自动生成买家评价回复草稿

## 🛠️ 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21 · Spring Boot · MyBatis-Plus · MySQL 8 · Redis 7 · JWT |
| AI 服务 | Python 3.12 · FastAPI · LangChain · DeepSeek（OpenAI 兼容协议）|
| 前端 | Vue 3 · Vite · Element Plus · ECharts（买家 / 卖家 / 管理三端独立）|
| 部署 | Docker · Docker Compose · Nginx 反向代理 · GitHub Actions CI |

## 🏗️ 系统架构

```text
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│  买家商城    │ │  卖家中心    │ │  管理后台    │     Vue3 + Nginx
│  :5173      │ │  :5176      │ │  :5175      │
└──────┬──────┘ └──────┬──────┘ └──────┬──────┘
       │               │               │
       └───────────────┼───────────────┘
                       ▼
              ┌────────────────┐      ┌────────────────┐
              │  mall-backend  │◄────►│   ai-service    │
              │  Java :8080    │ 调用  │ FastAPI :8001   │
              └────┬──────┬────┘      └───────┬────────┘
                   │      │                   │
              ┌────▼─┐ ┌──▼─────┐        ┌────▼────┐
              │ MySQL │ │ Redis  │        │DeepSeek │
              │  8.0  │ │  7     │        │  大模型  │
              └───────┘ └────────┘        └─────────┘
```

- 后端与 AI 服务通过内部接口（`X-Internal-Key` 校验）通信，AI 工具仅可查询当前用户数据，防越权
- 前端 Nginx 将 `/api` 请求反代到后端容器，SPA 路由回退 `index.html`

## 🚀 快速开始

### 方式一：Docker 一键部署（推荐）

> 需要已安装 Docker 与 Docker Compose，首次构建需拉取依赖，耗时较长

```bash
# 1. 配置 AI 服务密钥（AI 客服等能力需要，不配置则自动降级）
cp ai-service/.env.example ai-service/.env
# 编辑 ai-service/.env，填入 DEEPSEEK_API_KEY

# 2. 一键构建并启动（Linux / macOS）
./deploy.sh
# Windows PowerShell
.\deploy.ps1

# 3. 查看运行状态
docker compose ps
```

启动完成后访问：

| 服务 | 地址 |
|---|---|
| 买家商城 | http://localhost:5173 |
| 管理后台 | http://localhost:5175 |
| 卖家中心 | http://localhost:5176 |
| 后端 API | http://localhost:8080 |
| AI 服务健康检查 | http://localhost:8001/health |

### 方式二：本地开发

```bash
# 1. 启动依赖：MySQL(3306) 与 Redis(6379)，并执行 src/main/resources/sql/schema.sql 初始化
# 2. 启动后端（8080，配置 application.yml 连接本地 MySQL/Redis）
cd mall-backend && mvn spring-boot:run
# 3. 启动 AI 服务（8001）
cd ai-service && cp .env.example .env && uvicorn app.main:app --port 8001
# 4. 启动三个前端（5173 / 5175 / 5176）
cd mall-shop-web && npm i && npm run dev
cd mall-admin-web && npm i && npm run dev
cd mall-seller-web && npm i && npm run dev
```

### 👥 演示账号（密码统一 `123456`）

| 账号 | 角色 | 说明 |
|---|---|---|
| `zhangsan` | 买家 | 普通用户，含地址 / 订单数据 |
| `lisi` | 买家 | 普通用户 |
| `seller1` | 卖家 | 星耀数码旗舰店（手机数码）|
| `seller2` | 卖家 | 云裳美物生活馆（服饰 / 食品 / 个护）|
| `admin` | 管理员 | 管理后台 |

## 📁 项目结构

```text
mall/
├── mall-backend/        # Java 后端（Spring Boot + MyBatis-Plus）
│   └── src/main/resources/sql/   # schema.sql 全量初始化 + 增量迁移脚本
├── ai-service/          # AI 服务（FastAPI + LangChain + DeepSeek）
├── mall-shop-web/       # 买家商城前端（Vue3）
├── mall-seller-web/     # 卖家中心前端（Vue3）
├── mall-admin-web/      # 管理后台前端（Vue3）
├── docker-compose.yml   # 六容器一键编排（MySQL/Redis/后端/AI/三端前端）
├── deploy.sh / deploy.ps1   # 一键部署脚本
└── .github/workflows/ci.yml # GitHub Actions CI
```

## 🔄 CI / 部署

- **GitHub Actions**：每次推送自动执行后端 Maven 构建与三个前端构建，保证代码可构建
- **生产配置**：`application-prod.yml` 环境变量化（DB / Redis / AI 地址均可通过环境变量注入），Docker Compose 直接编排

## 🔒 安全说明

- `.env` 已加入 `.gitignore`，**切勿提交真实密钥**（DEEPSEEK_API_KEY 等）
- 内部接口通过 `X-Internal-Key` 鉴权，AI 工具查询强制按当前登录用户过滤，防数据越权
- 接口接入限流（Rate Limit），防止恶意刷接口

## 📄 License

本项目基于 [MIT License](LICENSE) 开源。
