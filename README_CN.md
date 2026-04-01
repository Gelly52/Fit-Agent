# Fit-Agent

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white) ![Spring Boot 3.5.10](https://img.shields.io/badge/Spring_Boot-3.5.10-6DB33F?logo=springboot&logoColor=white) ![Vue 3.5](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vuedotjs&logoColor=white) ![Spring AI 1.0.0](https://img.shields.io/badge/Spring_AI-1.0.0-6DB33F)

![RAG](https://img.shields.io/badge/RAG-Knowledge%20Enhanced-7C3AED) ![SSE](https://img.shields.io/badge/SSE-Streaming-0EA5E9) ![MCP](https://img.shields.io/badge/MCP-Tool%20Connected-2563EB) ![Agent](https://img.shields.io/badge/Agent-Task%20Execution-F97316)

**中文** | [English](README.md)

Fit-Agent 是一个面向健身场景的 AI 助手项目，当前仓库包含 Vue 3 前端、Spring Boot 后端、MCP 服务端与本地 RAG 数据集。系统已落地手机号验证码登录、流式聊天、Agent 执行、RAG 文档上传与检索、联网搜索、训练日志、身体指标记录，以及 RAG benchmark 评测。

## 功能概览

- 用户认证：`/user/code`、`/user/login`、`/user/logout`
- 实时聊天：`/chat/doChat`，通过 SSE 持续推送回复分片
- Agent 执行：`/agent/execute`、运行列表与详情查询
- RAG 能力：文档上传、手动检索、问答、配置读取、文档列表、`/rag/benchmark/evaluate`
- 联网搜索：基于 SearXNG 的搜索增强问答
- 健身记录：训练日志、身体指标近期摘要
- SSE 建连：ticket 模式，适配浏览器原生 `EventSource`

![前端页面预览](pics/fronted.png)

## 技术栈

- 前端：Vue 3、Vite 4、Element Plus、Axios
- 后端：Java 21、Spring Boot 3.5.10、Spring AI 1.0.0、MyBatis-Plus
- 数据与基础设施：MySQL、Redis、Redis Vector Store、SearXNG
- 协议与能力：SSE、MCP Client / MCP Server

## 目录结构

```text
Fit-Agent
├─ Fit-Agent-frontend          # Vue 3 + Vite 前端
├─ Fit-Agent-backend
│  ├─ mcp-client               # 业务后端，默认 7070
│  └─ mcp-server               # MCP 服务端
├─ datasets
│  └─ rag-mvp-fitness-v1       # RAG 知识文件、benchmark、来源索引
└─ docs                        # API 文档与实现流程文档
```

## 环境要求

- Node.js 18+
- Java 21
- Maven 3.9+
- MySQL
- Redis
- SearXNG
- 可用的 OpenAI 兼容模型服务

## 配置说明

当前代码中的默认开发配置如下：

- 前端：`http://127.0.0.1:5500`
- 后端：`http://127.0.0.1:7070`
- MySQL：`jdbc:mysql://127.0.0.1:5506/springai-items-mcp`
- Redis：`127.0.0.1:9379`
- SearXNG：`http://127.0.0.1:6080/search`
- Redis 向量索引：`lee-vectorstore`

模型与数据库连接可通过环境变量或 `.env` 提供，当前代码已使用到的关键项包括：

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`
- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

## 快速开始

### 1. 初始化数据库

导入初始化脚本：`Fit-Agent-backend/mcp-server/src/main/resources/sql/fit_agent_init.sql`

### 2. 启动后端服务

建议分别在两个终端启动：

```powershell
cd Fit-Agent-backend
mvn -pl mcp-server -am spring-boot:run
```

```powershell
cd Fit-Agent-backend
mvn -pl mcp-client -am spring-boot:run
```

### 3. 启动前端

```powershell
cd Fit-Agent-frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5500
```

访问：`http://127.0.0.1:5500/fitagent-vite.html`

## 文档与数据

- 接口文档：`docs/API_文档.md`
- RAG 流程文档：`docs/RAG检索增强生成实现流程.md`
- RAG benchmark：`datasets/rag-mvp-fitness-v1/benchmark/`
- 知识来源索引：`datasets/rag-mvp-fitness-v1/sources/source_index.md`

## 注意事项

- 受保护接口正式只信任 `headerUserToken`；前端默认从 cookie `user_token` 注入。
- SSE 连接需先调用 `POST /user/sse-ticket`，再使用 `GET /sse/connect?ticket=...` 建连。
- 当前 RAG 检索与 benchmark 均按登录用户隔离。
- 详细接口字段、示例与返回结构以 `docs/API_文档.md` 为准。
