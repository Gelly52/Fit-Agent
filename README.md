# Fit-Agent

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white) ![Spring Boot 3.5.10](https://img.shields.io/badge/Spring_Boot-3.5.10-6DB33F?logo=springboot&logoColor=white) ![Vue 3.5](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vuedotjs&logoColor=white) ![Spring AI 1.0.0](https://img.shields.io/badge/Spring_AI-1.0.0-6DB33F)

![RAG](https://img.shields.io/badge/RAG-Knowledge%20Enhanced-7C3AED) ![SSE](https://img.shields.io/badge/SSE-Streaming-0EA5E9) ![MCP](https://img.shields.io/badge/MCP-Tool%20Connected-2563EB) ![Agent](https://img.shields.io/badge/Agent-Task%20Execution-F97316)

[中文](README_CN.md) | **English**

Fit-Agent is an AI assistant project for fitness scenarios. The repository currently includes a Vue 3 frontend, a Spring Boot backend, an MCP server, and a local RAG dataset. The system already supports SMS-code login, streaming chat, agent execution, RAG document upload and retrieval, web search augmentation, training logs, body metrics tracking, and RAG benchmark evaluation.

## Features

- User authentication: `/user/code`, `/user/login`, `/user/logout`
- Real-time chat: `/chat/doChat`, with response chunks streamed over SSE
- Agent execution: `/agent/execute`, plus run list and run detail queries
- RAG capabilities: document upload, manual retrieval, question answering, config query, document list, and `/rag/benchmark/evaluate`
- Web search: search-augmented QA powered by SearXNG
- Fitness logging: training logs and recent body metrics summaries
- SSE connection: ticket-based handshake for native browser `EventSource`

![Frontend Preview](pics/fronted.png)

## Tech Stack

- Frontend: Vue 3, Vite 4, Element Plus, Axios
- Backend: Java 21, Spring Boot 3.5.10, Spring AI 1.0.0, MyBatis-Plus
- Data and infrastructure: MySQL, Redis, Redis Vector Store, SearXNG
- Protocols and capabilities: SSE, MCP Client / MCP Server

## Project Structure

```text
Fit-Agent
├─ Fit-Agent-frontend          # Vue 3 + Vite frontend
├─ Fit-Agent-backend
│  ├─ mcp-client               # Business backend, default port 7070
│  └─ mcp-server               # MCP server
├─ datasets
│  └─ rag-mvp-fitness-v1       # RAG knowledge files, benchmarks, source index
└─ docs                        # API docs and implementation flow docs
```

## Requirements

- Node.js 18+
- Java 21
- Maven 3.9+
- MySQL
- Redis
- SearXNG
- An available OpenAI-compatible model service

## Configuration

The default development configuration in the current codebase is:

- Frontend: `http://127.0.0.1:5500`
- Backend: `http://127.0.0.1:7070`
- MySQL: `jdbc:mysql://127.0.0.1:5506/springai-items-mcp`
- Redis: `127.0.0.1:9379`
- SearXNG: `http://127.0.0.1:6080/search`
- Redis vector index: `lee-vectorstore`

Model and database connections can be provided through environment variables or `.env`. The key items currently used by the code include:

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`
- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

## Quick Start

### 1. Initialize the database

Import the bootstrap script: `Fit-Agent-backend/mcp-server/src/main/resources/sql/fit_agent_init.sql`

### 2. Start backend services

It is recommended to start them in two separate terminals:

```powershell
cd Fit-Agent-backend
mvn -pl mcp-server -am spring-boot:run
```

```powershell
cd Fit-Agent-backend
mvn -pl mcp-client -am spring-boot:run
```

### 3. Start the frontend

```powershell
cd Fit-Agent-frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5500
```

Open: `http://127.0.0.1:5500/fitagent-vite.html`

## Docs and Data

- API documentation: `docs/API_文档.md`
- RAG flow documentation: `docs/RAG检索增强生成实现流程.md`
- RAG benchmarks: `datasets/rag-mvp-fitness-v1/benchmark/`
- Source index: `datasets/rag-mvp-fitness-v1/sources/source_index.md`

## Notes

- Protected endpoints officially trust only `headerUserToken`; the frontend injects it from the `user_token` cookie by default.
- To establish an SSE connection, call `POST /user/sse-ticket` first, then connect with `GET /sse/connect?ticket=...`.
- Current RAG retrieval and benchmarks are isolated by the logged-in user.
- For detailed fields, examples, and response structures, refer to `docs/API_文档.md`.
