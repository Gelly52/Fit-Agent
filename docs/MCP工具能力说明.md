# MCP 工具能力说明

## 1. 当前实现范围

当前仓库中的 MCP 能力主要由 `Fit-Agent-backend/mcp-server` 模块提供，服务端基于 Spring AI MCP Server 对外暴露结构化工具，适合 Agent 或其他 MCP Client 以工具调用的方式接入。

当前注册入口位于：

- `Fit-Agent-backend/mcp-server/src/main/java/com/itgeo/Application.java`

当前已注册的工具对象包括：

- `DateTool`
- `EmailTool`
- `TrainingLogTool`
- `BodyMetricsTool`
- `RagManageTool`

## 2. 当前工具能力

### 2.1 时间与通知工具

- 时间工具：支持获取当前时间，以及按时区查询指定城市对应时间。
- 邮件工具：支持查询默认发件邮箱，并向指定邮箱发送文本、Markdown 或 HTML 内容。

### 2.2 健身数据工具

- 训练日志工具：支持训练日志新增、修改、删除、动作明细写入，以及按条件查询训练记录。
- 身体指标工具：支持身体指标新增、修改、删除，以及按条件查询历史记录。

### 2.3 RAG 管理工具

- RAG 文档查询：支持按用户、文件名、文件哈希、向量状态等条件查询文档元数据。
- Benchmark 查询：支持按用户、数据集名称、状态等条件查询评测任务记录。

## 3. 当前使用说明

当前 MCP 工具更适合作为服务端结构化能力层使用，重点承接：

- Agent 场景下的工具调用
- 健身业务数据的结构化读写
- RAG 文档与评测记录的辅助查询

说明：

- 当前 `RagManageTool` 已按用户侧安全边界做了收敛，未向用户开放 RAG 配置写入能力。
- 详细接口与业务链路说明仍以 `docs/API接口文档.md`、`docs/聊天与Agent模式实现流程.md` 为准。
