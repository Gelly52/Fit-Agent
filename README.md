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
├─ Fit-Agent-frontend/
│  ├─ src/                          # Vue 3 application source
│  ├─ package.json                  # Frontend dependencies and scripts
│  ├─ vite.config.js                # Vite configuration
│  └─ fitagent-vite.html            # Frontend entry page
├─ Fit-Agent-backend/
│  ├─ mcp-client/                   # Business backend, default port 7070
│  │  └─ src/main/                  # Main source code and resources
│  ├─ mcp-server/                   # MCP server module
│  │  └─ src/main/resources/sql/    # Database bootstrap SQL
│  └─ pom.xml                       # Backend parent Maven project
├─ datasets
├─ docs
├─ pics
├─ .gitignore
├─ README.md
└─ README_CN.md
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
- Backend API (`mcp-client`): `http://127.0.0.1:7070`
- MCP Server: `http://127.0.0.1:9070`
- MySQL: `jdbc:mysql://127.0.0.1:5506/springai-items-mcp`
- Redis: `127.0.0.1:9379`
- SearXNG: `http://127.0.0.1:6080/search`
- Optional local embedding service: `http://127.0.0.1:7086`

Configuration examples and environment variable samples:

- `Fit-Agent-backend/mcp-client/src/main/resources/application-dev.example.yml`
- `Fit-Agent-backend/mcp-client/src/main/resources/.env.example`
- `Fit-Agent-backend/mcp-server/src/main/resources/application-dev.example.yml`
- `Fit-Agent-backend/mcp-server/src/main/resources/.env.example`

Optional local embedding service (`bge-m3`):

- Port: `7086`
- In the local embedding service directory, run:

```powershell
python -m uvicorn main:app --host 127.0.0.1 --port 7086
```

- Detailed configuration, vector index isolation, and implementation notes are documented in `docs/RAG检索增强生成实现流程.md`.

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

## Feature Overview

- Authentication and session flow: SMS-code login, token authentication, and SSE ticket-based connection support the full conversation lifecycle.
- Chat and Agent collaboration: the system supports streaming chat, Agent task execution, and run tracking for task-oriented interactions.
- RAG knowledge enhancement: users can upload documents, retrieve relevant content, ask knowledge-grounded questions, and run retrieval benchmarks. The embedding layer also supports a locally deployed `bge-m3` model.
- Search and fitness records: SearXNG-based web search augmentation is available alongside training log and body metrics recording features.
- Semantic chunking and hybrid retrieval: the project now supports configurable semantic chunking and hybrid retrieval, allowing the RAG pipeline to combine vector recall with keyword-based retrieval. After hybrid fusion, the pipeline can also optionally rerank fused candidates before returning the final TopK results.
- MCP tool support: the built-in MCP server currently provides tools for time lookup, email sending, training logs, body metrics, and RAG metadata / benchmark queries for structured agent-style operations. 

## Notes

- Protected endpoints officially trust only `headerUserToken`; the frontend injects it from the `user_token` cookie by default.
- To establish an SSE connection, call `POST /user/sse-ticket` first, then connect with `GET /sse/connect?ticket=...`.
- The local `bge-m3` embedding deployment details are documented in `docs/RAG检索增强生成实现流程.md`.
- Current RAG retrieval and benchmarks are isolated by the logged-in user.
- For detailed fields, examples, and response structures, refer to `docs/API接口文档.md`.
