# Fit-Agent 接口文档

> 版本：v2.8  
> 最后更新：2026-03-31  
> 维护原则：本文档以 `Fit-Agent-backend/mcp-client` 当前后端实现为准；前端占位接口与未来扩展接口单独标注，不作为当前正式契约。

---

## 目录

1. [当前接口概览](#当前接口概览)
2. [公共说明](#公共说明)
3. [当前已实现接口](#当前已实现接口)
4. [数据结构定义](#数据结构定义)
5. [前端占位接口与后续规划](#前端占位接口与后续规划)
6. [接口命名建议](#接口命名建议)
7. [更新日志](#更新日志)

---

## 当前接口概览

### 当前后端已实现接口

| 模块       | 接口路径                      | 请求方式 | 当前状态 | 返回方式                                    | 说明                                                             |
| ---------- | ----------------------------- | -------- | -------- | ------------------------------------------- | ---------------------------------------------------------------- |
| 用户认证   | `/user/code`                  | POST     | 已实现   | `LeeResult`                                 | 发送手机号登录验证码                                             |
| 用户认证   | `/user/login`                 | POST     | 已实现   | `LeeResult<UserLoginResponse>`              | 手机号验证码登录/自动注册                                        |
| 用户认证   | `/user/logout`                | POST     | 已实现   | `LeeResult`                                 | 退出当前登录会话                                                 |
| 用户认证   | `/user/sse-ticket`            | POST     | 已实现   | `LeeResult<SseTicketResponse>`              | 获取 SSE 短时连接票据                                            |
| 聊天       | `/chat/chatTest`              | POST     | 已实现   | 纯文本字符串                                | 测试同步聊天                                                     |
| 聊天       | `/chat/doChat`                | POST     | 已实现   | `LeeResult<ChatResponseEntity>` + SSE       | 普通聊天主入口，可通过 `ragEnabled/internetEnabled` 开启补充能力 |
| 聊天       | `/chat/records`               | GET      | 已实现   | `LeeResult<ChatRecordsResponse>`            | 查询当前登录用户聊天历史                                         |
| Agent      | `/agent/execute`              | POST     | 已实现   | `LeeResult<AgentExecuteAckResponse>` + SSE  | Agent 任务受理主入口                                             |
| Agent      | `/agent/runs`                 | GET      | 已实现   | `LeeResult<List<AgentRunListItemResponse>>` | 查询当前登录用户最近的 Agent 运行列表                            |
| Agent      | `/agent/runs/{runId}`         | GET      | 已实现   | `LeeResult<AgentRunDetailResponse>`         | 查询当前登录用户指定的 Agent 运行详情                            |
| RAG 知识库 | `/rag/uploadRagDoc`           | POST     | 已实现   | `LeeResult<List<Document>>`                 | 上传用户知识库文档                                               |
| RAG 知识库 | `/rag/doSearch`               | GET      | 已实现   | `LeeResult<List<Document>>`                 | 手动向量检索                                                     |
| RAG 知识库 | `/rag/search`                 | POST     | 已实现   | `LeeResult<ChatResponseEntity>` + SSE       | 手动 RAG 问答入口                                                |
| RAG 知识库 | `/rag/docs`                   | GET      | 已实现   | `LeeResult<List<RagDocumentItem>>`          | 查询当前用户已上传文档                                           |
| RAG 知识库 | `/rag/config`                 | GET/POST | 已实现   | `LeeResult<RagConfigResponse>`              | 查询当前手动 RAG 配置                                            |
| RAG 知识库 | `/rag/benchmark/evaluate`     | POST     | 已实现   | `LeeResult<RagBenchmarkEvaluateResponse>`   | 评测当前用户知识库检索对目标文件的命中率                         |
| 联网搜索   | `/internet/test`              | GET      | 已实现   | `LeeResult<List<SearchResult>>`             | 联网搜索测试                                                     |
| 联网搜索   | `/internet/search`            | POST     | 已实现   | `LeeResult<ChatResponseEntity>` + SSE       | 联网搜索问答入口                                                 |
| 训练日志   | `/training/log`               | POST     | 已实现   | `LeeResult`                                 | 新增或覆盖当天训练记录                                           |
| 训练日志   | `/training/recent`            | GET      | 已实现   | `LeeResult<List<DateSummaryItem>>`          | 查询近期训练摘要                                                 |
| 身体指标   | `/body-metrics/log`           | POST     | 已实现   | `LeeResult`                                 | 新增或覆盖当天身体指标                                           |
| 身体指标   | `/body-metrics/recent`        | GET      | 已实现   | `LeeResult<List<DateSummaryItem>>`          | 查询近期身体指标摘要                                             |
| SSE 通信   | `/sse/connect`                | GET      | 已实现   | `text/event-stream`                         | 使用 ticket 建立 SSE 连接                                        |
| SSE 通信   | `/sse/sendMessage`            | GET      | 已实现   | 纯文本字符串                                | 向当前登录连接发送普通消息                                       |
| SSE 通信   | `/sse/sendMessageAdd`         | GET      | 已实现   | 纯文本字符串                                | 向当前登录连接发送追加消息                                       |
| SSE 通信   | `/sse/sendMessageAll`         | GET      | 已实现   | 纯文本字符串                                | SSE 群发消息                                                     |
| 测试       | `/hello/world`                | GET      | 已实现   | 纯文本字符串                                | Hello World                                                      |
| 测试       | `/hello/chat`                 | GET      | 已实现   | 纯文本字符串                                | 简单聊天测试                                                     |
| 测试       | `/hello/chat/stream/response` | GET      | 已实现   | `Flux<ChatResponse>`                        | 流式响应测试                                                     |
| 测试       | `/hello/chat/stream/str`      | GET      | 已实现   | `Flux<String>`                              | 流式字符串测试                                                   |

### 当前未实现但前端存在占位调用的接口

当前暂无仍未落地的前端占位接口。

---

## 公共说明

### 基础 URL

当前后端开发环境端口来自 `Fit-Agent-backend/mcp-client/src/main/resources/application-dev.yml`：

```text
开发环境: http://127.0.0.1:7070
```

说明：

- 前端运行时也指向 `http://127.0.0.1:7070`
- 旧文档中的 `http://localhost:8080` 已不再作为当前开发环境基准

### 跨域与凭证

后端当前允许跨域，并开启 `allowCredentials(true)`：

- 允许来源配置见：`Fit-Agent-backend/mcp-client/src/main/java/com/itgeo/CorsConfig.java`
- 开发环境域名配置见：`Fit-Agent-backend/mcp-client/src/main/resources/application-dev.yml`

当前允许的主要前端来源：

```text
http://127.0.0.1:5500
http://localhost:5500
https://jovoj.eu.cc
```

### 鉴权与请求头

#### 统一 token 鉴权规则

当前后端已接入统一 token 鉴权，服务端正式只信任 `headerUserToken`。

- 统一拦截范围：`/user/**`、`/chat/**`、`/agent/**`、`/rag/**`、`/internet/**`、`/sse/**`、`/training/**`、`/body-metrics/**`

- 鉴权白名单：`/user/code`、`/user/login`、`/hello/**`、`/sse/connect`、`/error`
- 鉴权失败时，HTTP 状态码为 `401`
- 鉴权失败响应体仍为 `LeeResult` JSON，其中业务状态码为 `502`

#### 当前兼容字段与废弃身份来源

| 字段 / 位置              | 当前状态       | 说明                                                                        |
| ------------------------ | -------------- | --------------------------------------------------------------------------- |
| `headerUserToken`        | 正式使用       | 受保护 HTTP 接口的正式鉴权依据                                              |
| `headerUserId`           | 可发送但不可信 | 前端兼容字段，服务端不以此作为真实身份依据                                  |
| 请求体 `currentUserName` | 可传但不可信   | `chat` / `agent` / `rag` / `internet` 控制器会用当前登录用户 `userKey` 覆盖 |
| 旧 SSE 查询参数 `userId` | 已废弃         | `/sse/connect` 已切换为 `ticket` 建连，不再接受 `userId`                    |

#### SSE ticket 建连规则

由于浏览器原生 `EventSource` 不能复用普通 HTTP 请求中的 `headerUserToken` 注入链，当前 SSE 建连采用短时 ticket 方案：

1. 先通过 `POST /user/sse-ticket` 获取连接票据
2. 再通过 `GET /sse/connect?ticket=...` 建立 SSE 连接
3. `ticket` 有效期为 60 秒
4. `ticket` 只能成功消费一次，过期或消费后需重新申请

### 请求体与响应体说明

当前接口返回方式分为 4 类：

#### 1. `LeeResult` 统一封装

部分接口返回以下结构：

```json
{
  "status": 200,
  "msg": "OK",
  "data": {}
}
```

#### 2. 纯文本字符串

例如：

- `/chat/chatTest`
- `/sse/sendMessage`
- `/sse/sendMessageAdd`
- `/sse/sendMessageAll`
- `/hello/world`
- `/hello/chat`

#### 3. HTTP + SSE 双通道返回结果

当前聊天相关接口分为两类：

- `/chat/doChat`、`/rag/search`、`/internet/search`
  - HTTP `LeeResult.data` 中会返回最终 `ChatResponseEntity`
  - 同时继续通过 SSE 推送 `add` / `finish` 事件，供前端做流式展示
- `/agent/execute`
  - HTTP 先返回 `AgentExecuteAckResponse` 受理结果
  - 后续步骤事件、流式内容和最终结果继续通过 SSE 推送

#### 4. 流式测试接口

以下接口返回响应式流对象：

- `/hello/chat/stream/response`
- `/hello/chat/stream/str`

### 状态码说明

以下状态码仅适用于返回 `LeeResult` 的接口：

| 状态码 | 说明             |
| ------ | ---------------- |
| 200    | 成功             |
| 500    | 错误             |
| 501    | Bean 验证错误    |
| 502    | Token 错误       |
| 555    | 异常抛出         |
| 556    | 用户 QQ 校验异常 |
| 557    | CAS 登录校验异常 |

---

## 当前已实现接口

### 0. 用户认证模块 `/user`

#### 0.1 发送登录验证码

**接口路径：** `POST /user/code`

**功能描述：**

- 校验手机号格式
- 生成 6 位验证码并写入 Redis
- 验证码有效期 5 分钟
- 当前接口不再向前端返回验证码；开发环境仅记录后端日志，生产环境不输出明文验证码

**请求体：**

| 参数名 | 类型   | 必填 | 说明           |
| ------ | ------ | ---- | -------------- |
| phone  | string | 是   | 中国大陆手机号 |

**请求示例：**

```json
{
  "phone": "13800138000"
}
```

**响应类型：** `LeeResult`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": null
}
```

---

#### 0.2 手机号验证码登录

**接口路径：** `POST /user/login`

**功能描述：**

- 校验手机号与验证码
- 验证码通过后立即删除 Redis 中的验证码缓存
- 若手机号未注册，服务端会自动创建用户
- 登录成功后写入 `t_user_login_session`
- 当前登录会话有效期 7 天

**请求体：**

| 参数名 | 类型   | 必填 | 说明           |
| ------ | ------ | ---- | -------------- |
| phone  | string | 是   | 中国大陆手机号 |
| code   | string | 是   | 6 位验证码     |

**请求示例：**

```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

**响应类型：** `LeeResult<UserLoginResponse>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "token": "6f5e0d2f7d0d46f0a5f7d9d65a7f0a12",
    "expiresAt": "2026-03-31 10:00:00",
    "newUser": false,
    "userInfo": {
      "id": "u_8b5c4f9f7d8b4f7bb7c6a0e4d6d0f123",
      "userId": 1,
      "userKey": "u_8b5c4f9f7d8b4f7bb7c6a0e4d6d0f123",
      "username": "13800138000",
      "nickname": "用户8000",
      "phone": "13800138000"
    }
  }
}
```

---

#### 0.3 退出登录

**接口路径：** `POST /user/logout`

**鉴权要求：**

- 需要 `headerUserToken`
- 受统一 token 鉴权保护

**功能描述：**

- 撤销当前登录会话
- 请求体为空
- 若当前 token 已无法匹配有效会话，服务端会直接返回成功

**响应类型：** `LeeResult`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": null
}
```

---

#### 0.4 获取 SSE 连接票据

**接口路径：** `POST /user/sse-ticket`

**鉴权要求：**

- 需要 `headerUserToken`
- 受统一 token 鉴权保护

**功能描述：**

- 基于当前登录会话生成短时 SSE 连接票据
- 返回 `ticket` 与剩余有效期
- `ticket` 有效期为 60 秒，且只能消费一次

**响应类型：** `LeeResult<SseTicketResponse>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "ticket": "6f5e0d2f7d0d46f0a5f7d9d65a7f0a12",
    "expiresInSeconds": 60
  }
}
```

### 1. 聊天模块 `/chat`

#### 1.1 测试聊天

**接口路径：** `POST /chat/chatTest`

**功能描述：**

- 使用普通聊天能力直接返回 AI 文本回复
- 当前控制器实际接收 `ChatEntity`，但服务层当前只使用 `message`

**请求体：**

| 参数名          | 类型   | 必填 | 说明                     |
| --------------- | ------ | ---- | ------------------------ |
| currentUserName | string | 否   | 当前后端未使用，预留字段 |
| message         | string | 是   | 用户消息                 |
| botMsgId        | string | 否   | 当前后端未使用，预留字段 |

**请求示例：**

```json
{
  "currentUserName": "user123",
  "message": "你好",
  "botMsgId": "msg_001"
}
```

**响应类型：** 纯文本字符串

**响应示例：**

```text
你好！我是 GoGo，很高兴为你服务。
```

---

#### 1.2 流式聊天对话

**接口路径：** `POST /chat/doChat`

**鉴权要求：**

- 需要 `headerUserToken`
- 服务端只信任登录态，不信任前端传入的身份字段

**功能描述：**

- 该接口是普通聊天主入口
- 主模式固定为 `chat`
- 可通过 `ragEnabled` 或 `internetEnabled` 开启一种补充能力
- 当前不支持同时开启知识库增强与联网补充；两者同时为 `true` 时会直接报错
- 服务端会用当前登录用户的 `userKey` 覆盖请求体中的 `currentUserName`
- 如果当前请求其实是 `/agent/execute` 的前端 fallback，且同一 `userId + botMsgId` 已被 Agent 链路受理或落库，控制器会短路返回 `LeeResult.ok()`，避免重复执行

**请求体：**

| 参数名          | 类型    | 必填 | 说明                                                   |
| --------------- | ------- | ---- | ------------------------------------------------------ |
| currentUserName | string  | 否   | 兼容旧字段；即使传入也会被服务端覆盖                   |
| message         | string  | 是   | 用户消息                                               |
| botMsgId        | string  | 否   | 前端用于归并流式消息的机器人消息 ID，建议传            |
| sessionCode     | string  | 否   | 会话编码；用于续聊时复用当前 `chat` 会话               |
| clientRequestId | string  | 否   | 客户端请求标识；当前后端未参与业务判断                 |
| ragEnabled      | boolean | 否   | 是否开启知识库增强；开启后当前轮 `sourceType = rag`    |
| internetEnabled | boolean | 否   | 是否开启联网补充；开启后当前轮 `sourceType = internet` |

**请求示例：**

```json
{
  "message": "帮我制定一个健身计划",
  "botMsgId": "msg_001",
  "sessionCode": "cs_001",
  "ragEnabled": false,
  "internetEnabled": false
}
```

**HTTP 响应：** `LeeResult<ChatResponseEntity>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "message": "建议你每周安排 4 次训练，采用上肢/下肢分化。",
    "botMsgId": "msg_001",
    "runId": null,
    "chatSessionId": 2001,
    "sessionCode": "cs_001",
    "sourceType": "chat",
    "sources": null,
    "sceneType": "chat"
  }
}
```

**SSE 行为：**

- `add`：推送结构化流式增量事件，当前负载为 `ChatStreamChunkResponse`
- `finish`：返回完整 `ChatResponseEntity` JSON 字符串；普通 `chat` 成功路径中 `runId` 通常为 `null`

---

#### 1.3 聊天历史记录

**接口路径：** `GET /chat/records`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 查询当前登录用户的聊天历史
- `who` 仅用于兼容前端旧调用，真实查询身份始终以当前登录用户为准
- 不传 `sessionId` 时返回最近会话列表，并按 `updatedAt` 倒序
- 传了 `sessionId` 时只返回该会话
- 会话内消息按 `seqNo` 正序返回

**请求参数：**

| 参数名    | 类型   | 必填 | 说明                                   |
| --------- | ------ | ---- | -------------------------------------- |
| who       | string | 否   | 兼容旧参数，当前后端忽略其身份含义     |
| sessionId | number | 否   | 指定会话 ID；传入时仅返回该会话        |
| limit     | number | 否   | 最近会话条数，默认 `20`，当前最大 `50` |

**请求示例：**

```text
GET /chat/records?who=u_demo&limit=20
GET /chat/records?sessionId=2001
```

**响应类型：** `LeeResult<ChatRecordsResponse>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "userId": 1,
    "totalSessions": 1,
    "sessions": [
      {
        "sessionId": 2001,
        "sessionCode": "cs_001",
        "sceneType": "chat",
        "title": "帮我制定一个健身计划",
        "lastBotMsgId": "msg_001",
        "createdAt": "2026-03-30T18:00:00",
        "updatedAt": "2026-03-30T18:05:00",
        "messages": [
          {
            "sessionId": 2001,
            "sessionCode": "cs_001",
            "sceneType": "chat",
            "messageId": 3001,
            "seqNo": 1,
            "role": "user",
            "messageType": "text",
            "sourceType": "chat",
            "content": "帮我制定一个健身计划",
            "botMsgId": null,
            "sourcesJson": null,
            "createdAt": "2026-03-30T18:00:00"
          }
        ]
      }
    ]
  }
}
```

---

### 2. Agent 模块 `/agent`

#### 2.1 Agent 任务受理

**接口路径：** `POST /agent/execute`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护
- 发起前必须先完成 `/user/sse-ticket` + `/sse/connect` 建链

**功能描述：**

- 该接口只负责“受理任务”，不会等待最终回答生成后再返回
- 服务端会先校验当前登录会话对应的 SSE 通道是否在线
- 同一 `userId + botMsgId` 会先做幂等检查，已存在时直接返回已有 run 的 ack
- 同一登录 `sessionId` 同时只允许一个 Agent 任务执行
- 受理阶段会先创建或复用 `agent` 会话，再写入用户消息、assistant 占位消息、run 主记录和固定步骤
- 真正执行逻辑在事务提交后异步派发
- 当前 Agent 也支持通过 `ragEnabled` 或 `internetEnabled` 开启一种补充能力
- 当前不支持同时开启知识库增强与联网补充；两者同时为 `true` 时会直接报错

**请求体：**

| 参数名          | 类型    | 必填 | 说明                                                        |
| --------------- | ------- | ---- | ----------------------------------------------------------- |
| currentUserName | string  | 否   | 兼容旧字段；即使传入也会被服务端当前登录用户 `userKey` 覆盖 |
| message         | string  | 是   | 用户任务内容                                                |
| botMsgId        | string  | 是   | 机器人消息 ID；作为幂等与流式归并关键字段，当前不能为空     |
| sessionCode     | string  | 否   | 会话编码；用于续聊时复用当前 `agent` 会话                   |
| clientRequestId | string  | 否   | 客户端请求标识；当前后端未参与业务判断                      |
| ragEnabled      | boolean | 否   | 是否开启知识库增强；开启后当前轮 `sourceType = rag`         |
| internetEnabled | boolean | 否   | 是否开启联网补充；开启后当前轮 `sourceType = internet`      |

**请求示例：**

```json
{
  "message": "帮我分析今天的腿部训练安排",
  "botMsgId": "agent_001",
  "sessionCode": "cs_agent_001",
  "clientRequestId": "req_001",
  "ragEnabled": false,
  "internetEnabled": true
}
```

**HTTP 响应：** `LeeResult<AgentExecuteAckResponse>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "runId": 1001,
    "chatSessionId": 2001,
    "sessionCode": "cs_agent_001",
    "botMsgId": "agent_001",
    "status": "pending",
    "duplicate": false
  }
}
```

**SSE 行为：**

- `custom_event`：推送固定 5 步的 Agent 步骤事件，数据结构为 `AgentStepEvent`
- `add`：推送结构化流式增量事件，当前负载为 `ChatStreamChunkResponse`
- `finish`：成功路径返回带 `runId` 的 `ChatResponseEntity`；在聊天完成前失败时返回 `AgentFinishResponse`

**额外说明：**

- 若当前 SSE 未建立，会返回 `LeeResult.errorMsg("SSE通道未建立，请先连接后再发起任务")`
- 若同一登录会话已有运行中的 Agent 任务，会返回 `LeeResult.errorMsg("当前已有任务执行中，请稍后再试")`
- 若重复提交同一个 `botMsgId`，会返回已有任务的 ack，且 `duplicate = true`

---

#### 2.2 查询 Agent 运行列表

**接口路径：** `GET /agent/runs`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 查询当前登录用户最近的 Agent 运行列表
- 可按 `status` 做可选过滤
- `limit` 缺省为 `10`，当前服务端最大限制为 `50`
- 返回结果按 `runId` 倒序

**请求参数：**

| 参数名 | 类型   | 必填 | 说明                                                |
| ------ | ------ | ---- | --------------------------------------------------- |
| status | string | 否   | 可选状态过滤，例如 `pending/running/success/failed` |
| limit  | number | 否   | 返回条数，默认 `10`，当前最大 `50`                  |

**请求示例：**

```text
GET /agent/runs
GET /agent/runs?status=running&limit=10
```

**响应类型：** `LeeResult<List<AgentRunListItemResponse>>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": [
    {
      "runId": 1001,
      "chatSessionId": 2001,
      "sessionCode": "cs_agent_001",
      "botMsgId": "agent_001",
      "requestText": "帮我分析今天的腿部训练安排",
      "status": "running",
      "errorMessage": null,
      "createdAt": "2026-03-31T10:00:00",
      "startedAt": "2026-03-31T10:00:01",
      "finishedAt": null
    }
  ]
}
```

---

#### 2.3 查询 Agent 运行详情

**接口路径：** `GET /agent/runs/{runId}`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 查询当前登录用户指定的 Agent 运行详情
- 若 `runId` 不存在或不属于当前用户，后端会返回业务错误
- 返回结果包含 run 主记录与步骤列表

**路径参数：**

| 参数名 | 类型   | 必填 | 说明          |
| ------ | ------ | ---- | ------------- |
| runId  | number | 是   | Agent 运行 ID |

**请求示例：**

```text
GET /agent/runs/1001
```

**响应类型：** `LeeResult<AgentRunDetailResponse>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "runId": 1001,
    "chatSessionId": 2001,
    "sessionCode": "cs_agent_001",
    "botMsgId": "agent_001",
    "requestText": "帮我分析今天的腿部训练安排",
    "status": "success",
    "resultJson": "{\"message\":\"建议适当降低训练量并加强恢复\"}",
    "errorMessage": null,
    "createdAt": "2026-03-31T10:00:00",
    "startedAt": "2026-03-31T10:00:01",
    "finishedAt": "2026-03-31T10:00:08",
    "steps": [
      {
        "stepNo": 1,
        "stepName": "解析任务意图",
        "stepStatus": "success",
        "toolName": null,
        "inputJson": "{\"phase\":\"intent\"}",
        "outputJson": "{\"status\":\"ok\"}",
        "errorMessage": null,
        "startedAt": "2026-03-31T10:00:01",
        "finishedAt": "2026-03-31T10:00:01"
      }
    ]
  }
}
```

---

### 3. RAG 知识库模块 `/rag`

#### 2.1 上传 RAG 文档

**接口路径：** `POST /rag/uploadRagDoc`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 读取上传文件
- 使用 `TextReader` 解析文本
- 将 `fileName`、`userId` 写入 `Document.metadata`
- 使用 `TokenTextSplitter` 切分后写入 Redis 向量库
- HTTP 返回值中的 `data` 为原始读取出的 `Document` 列表

**请求类型：** `multipart/form-data`

**表单参数：**

| 参数名 | 类型          | 必填 | 说明     |
| ------ | ------------- | ---- | -------- |
| file   | MultipartFile | 是   | 上传文件 |

**说明：**

- 前端当前界面限制为 `.txt` 且不超过 10MB
- 但后端当前代码未显式做扩展名和大小校验
- 因此“仅支持 `.txt` / 10MB”目前属于前端限制，不属于后端硬性契约

**请求示例：**

```text
Content-Type: multipart/form-data
file: [文件内容]
```

**响应类型：** `LeeResult`

**响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": [
    {
      "text": "文档原始内容...",
      "metadata": {
        "fileName": "guide.txt",
        "userId": 1
      }
    }
  ]
}
```

---

#### 2.2 向量搜索

**接口路径：** `GET /rag/doSearch`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 根据 `question` 在 Redis 向量库中做相似度搜索
- 当前控制器固定 `topK = 4`
- 服务层当前会先扩大召回，再按当前登录用户的 `metadata.userId` 做结果过滤

**请求参数：**

| 参数名   | 类型   | 必填 | 说明     |
| -------- | ------ | ---- | -------- |
| question | string | 是   | 搜索问题 |

**请求示例：**

```text
GET /rag/doSearch?question=如何增肌
```

**响应类型：** `LeeResult`

**响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": [
    {
      "text": "增肌的关键在于训练强度、恢复和营养摄入。",
      "metadata": {
        "fileName": "guide.txt",
        "userId": 1
      }
    }
  ]
}
```

---

#### 3.3 RAG 增强聊天

**接口路径：** `POST /rag/search`

**鉴权要求：**

- 需要 `headerUserToken`
- 服务端只信任登录态，不信任前端传入的身份字段

**功能描述：**

- 这是手动 RAG 问答入口
- 控制器会先按当前登录用户执行向量检索，再把检索结果传给 `ChatService.doChatRagSearch(...)`
- 当前前端主聊天链路更推荐走 `/chat/doChat + ragEnabled=true` 或 `/agent/execute + ragEnabled=true`
- 服务端会用当前登录用户的 `userKey` 覆盖请求体中的 `currentUserName`

**请求体：**

| 参数名          | 类型   | 必填 | 说明                                       |
| --------------- | ------ | ---- | ------------------------------------------ |
| currentUserName | string | 否   | 兼容旧字段；前端可不传，即使传入也会被覆盖 |
| message         | string | 是   | 用户问题                                   |
| botMsgId        | string | 否   | 机器人消息 ID，建议传                      |
| sessionCode     | string | 否   | 会话编码；若与当前 `chat` 场景兼容则可续聊 |

**请求示例：**

```json
{
  "message": "深蹲的正确姿势是什么",
  "botMsgId": "msg_002",
  "sessionCode": "cs_002"
}
```

**HTTP 响应：** `LeeResult<ChatResponseEntity>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "message": "深蹲时应保持核心收紧、脊柱中立。",
    "botMsgId": "msg_002",
    "runId": null,
    "chatSessionId": 2002,
    "sessionCode": "cs_002",
    "sourceType": "rag",
    "sources": [
      {
        "title": "guide.txt",
        "snippet": "深蹲时膝盖与脚尖方向保持一致。",
        "url": "",
        "extra": ""
      }
    ],
    "sceneType": "chat"
  }
}
```

**SSE 行为：**

- `add`：推送结构化流式增量事件，当前负载为 `ChatStreamChunkResponse`
- `finish`：返回完整 `ChatResponseEntity` JSON 字符串；手动 RAG 成功路径中 `runId` 通常为 `null`

---

#### 3.4 查询当前用户已上传文档

**接口路径：** `GET /rag/docs`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 查询当前登录用户已经上传并写入索引的 RAG 文档列表
- 仅返回当前用户可见的文档

**响应类型：** `LeeResult<List<RagDocumentItem>>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": [
    {
      "id": 1,
      "fileName": "guide.txt",
      "sourceCount": 3,
      "chunkCount": 8,
      "status": "READY",
      "createdAt": "2026-03-30T18:20:00"
    }
  ]
}
```

---

#### 3.5 查询手动 RAG 配置

**接口路径：** `GET /rag/config`

**兼容路径：** `POST /rag/config`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 查询当前手动 RAG 配置
- `POST /rag/config` 仅用于兼容旧调用方式，返回内容与 `GET /rag/config` 一致

**响应类型：** `LeeResult<RagConfigResponse>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "defaultTopK": 4,
    "maxTopK": 10,
    "filterScanLimit": 50,
    "userIsolationEnabled": true,
    "isolationStrategy": "vectorstore_filterExpression_userId"
  }
}
```

---

#### 3.6 RAG 基准评测

**接口路径：** `POST /rag/benchmark/evaluate`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 评测当前登录用户的 RAG 检索是否能把目标文件召回到 topK
- 当前实现只做“检索命中率评测”，不做答案正确性评测
- 每次评测会先向 `t_rag_benchmark_run` 写入一条 `running` 记录，成功后更新为 `success`，失败时更新为 `failed`
- 服务端会先读取当前手动 RAG 配置，再计算本次评测实际执行的 `topK`
- 对每一道题，服务端调用 `documentService.doSearch(question, userId, topK)`
- 若返回结果中任一文档的 `metadata.fileName == expectedFileName`，则该题判定为命中

**请求体：**

| 参数名      | 类型                            | 必填 | 说明                                              |
| ----------- | ------------------------------- | ---- | ------------------------------------------------- |
| datasetName | string                          | 否   | 本次评测的数据集名称，例如 `smoke-v1`             |
| topK        | number                          | 否   | 期望召回条数；不传或小于等于 0 时走默认值         |
| questions   | `RagBenchmarkQuestionRequest[]` | 是   | 评测题目列表，至少 1 条，当前服务端最多支持 20 条 |

**questions 元素字段：**

| 参数名           | 类型   | 必填 | 说明                                                  |
| ---------------- | ------ | ---- | ----------------------------------------------------- |
| question         | string | 是   | 问题文本                                              |
| expectedFileName | string | 是   | 预期命中的文件名，建议直接从 `/rag/docs` 的响应中复制 |

**请求示例：**

```json
{
  "datasetName": "smoke-v1",
  "topK": 4,
  "questions": [
    {
      "question": "深蹲的核心动作要点是什么？",
      "expectedFileName": "squat-guide.txt"
    },
    {
      "question": "蛋白质摄入建议是多少？",
      "expectedFileName": "nutrition-notes.txt"
    }
  ]
}
```

**响应类型：** `LeeResult<RagBenchmarkEvaluateResponse>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "runId": 12,
    "datasetName": "smoke-v1",
    "questionCount": 2,
    "topK": 4,
    "status": "success",
    "hitCount": 1,
    "hitRate": 0.5,
    "userIsolationEnabled": true,
    "isolationStrategy": "vectorstore_filterExpression_userId",
    "results": [
      {
        "index": 1,
        "question": "深蹲的核心动作要点是什么？",
        "expectedFileName": "squat-guide.txt",
        "hit": true,
        "retrievedFileNames": ["squat-guide.txt", "warmup.txt"],
        "topHitPreview": "深蹲时膝盖与脚尖方向保持一致……"
      },
      {
        "index": 2,
        "question": "蛋白质摄入建议是多少？",
        "expectedFileName": "nutrition-notes.txt",
        "hit": false,
        "retrievedFileNames": ["meal-plan.txt", "warmup.txt"],
        "topHitPreview": "建议结合体重与训练目标安排饮食……"
      }
    ]
  }
}
```

**说明：**

- `topK` 返回的是本次评测实际执行值
- `hitRate` 当前使用 `0~1` 小数表示，例如 `0.5`
- 当前成功路径下，响应体内部的 `status` 固定为 `success`
- 请求校验失败时返回 `LeeResult.errorMsg(...)`，运行期异常时返回 `LeeResult.errorException(...)`

---

### 4. 联网搜索模块 `/internet`

#### 4.1 联网搜索测试

**接口路径：** `GET /internet/test`

**功能描述：**

- 调用 SearXNG 搜索并直接返回搜索结果

**请求参数：**

| 参数名 | 类型   | 必填 | 说明       |
| ------ | ------ | ---- | ---------- |
| query  | string | 是   | 搜索关键词 |

**请求示例：**

```text
GET /internet/test?query=健身减脂方法
```

**响应类型：** `LeeResult<List<SearchResult>>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": [
    {
      "title": "搜索结果标题",
      "url": "https://example.com",
      "content": "搜索结果摘要...",
      "score": 0.82
    }
  ]
}
```

---

#### 4.2 联网搜索聊天

**接口路径：** `POST /internet/search`

**鉴权要求：**

- 需要 `headerUserToken`
- 服务端只信任登录态，不信任前端传入的身份字段

**功能描述：**

- 这是手动联网问答入口
- 控制器会先做联网搜索，再调用 `ChatService.doInternetSearch(...)`
- 当前前端主聊天链路更推荐走 `/chat/doChat + internetEnabled=true` 或 `/agent/execute + internetEnabled=true`
- 服务端会用当前登录用户的 `userKey` 覆盖请求体中的 `currentUserName`

**请求体：**

| 参数名          | 类型   | 必填 | 说明                                           |
| --------------- | ------ | ---- | ---------------------------------------------- |
| currentUserName | string | 否   | 兼容旧字段；前端可不传，即使传入也会被覆盖     |
| message         | string | 是   | 用户问题                                       |
| botMsgId        | string | 否   | 机器人消息 ID，建议传                          |
| sessionCode     | string | 否   | 会话编码；若与当前 `chat` 场景兼容则可继续续聊 |

**请求示例：**

```json
{
  "message": "最新的健身研究有哪些",
  "botMsgId": "msg_003",
  "sessionCode": "cs_003"
}
```

**HTTP 响应：** `LeeResult<ChatResponseEntity>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": {
    "message": "近期研究显示，力量训练与高蛋白摄入结合对增肌更有利。",
    "botMsgId": "msg_003",
    "runId": null,
    "chatSessionId": 2003,
    "sessionCode": "cs_003",
    "sourceType": "internet",
    "sources": [
      {
        "title": "研究综述",
        "snippet": "力量训练结合高蛋白饮食有助于肌肥大。",
        "url": "https://example.com",
        "extra": ""
      }
    ],
    "sceneType": "chat"
  }
}
```

**SSE 行为：**

- `add`：推送结构化流式增量事件，当前负载为 `ChatStreamChunkResponse`
- `finish`：返回完整 `ChatResponseEntity` JSON 字符串；手动联网成功路径中 `runId` 通常为 `null`

---

### 5. SSE 实时通信模块 `/sse`

#### 5.1 建立 SSE 连接

**接口路径：** `GET /sse/connect`

**功能描述：**

- 当前接口使用 `ticket` 建立 SSE 连接
- `ticket` 需要先通过 `POST /user/sse-ticket` 获取
- `/sse/connect` 自身不直接读取 `headerUserToken`，而是消费一次性 ticket
- ticket 校验成功后，连接会绑定到当前登录会话对应的 SSE 客户端标识

**请求参数：**

| 参数名 | 类型   | 必填 | 说明              |
| ------ | ------ | ---- | ----------------- |
| ticket | string | 是   | 短时 SSE 连接票据 |

**请求示例：**

```text
GET /sse/connect?ticket=6f5e0d2f7d0d46f0a5f7d9d65a7f0a12
```

**响应类型：** `text/event-stream`

**额外说明：**

- `ticket` 有效期为 60 秒
- `ticket` 只能成功消费一次
- 过期、空值或重复消费后，需要重新调用 `/user/sse-ticket`

#### 5.2 当前事件模型

需要区分“浏览器原生事件”和“后端主动发送的业务事件”：

| 事件名         | 类型           | 当前状态   | 说明                                                     |
| -------------- | -------------- | ---------- | -------------------------------------------------------- |
| `open`         | 浏览器原生事件 | 当前可见   | 连接建立时浏览器侧触发，不是后端主动命名发送             |
| `error`        | 浏览器原生事件 | 当前可见   | 连接异常或断开时浏览器侧触发                             |
| `message`      | 业务事件       | 已实现     | 普通消息事件                                             |
| `add`          | 业务事件       | 已实现     | 结构化流式增量事件，当前负载为 `ChatStreamChunkResponse` |
| `finish`       | 业务事件       | 已实现     | 返回最终完整结果；不同链路下负载结构可能不同             |
| `custom_event` | 业务事件       | 已实现     | Agent 步骤事件，当前主要由 `/agent/execute` 异步链路发送 |
| `done`         | 预留枚举值     | 未实际发送 | 当前后端没有业务代码使用                                 |

#### 5.3 `finish` 事件数据结构

当前 `finish` 事件主要有两种负载：

1. 聊天成功路径：`ChatResponseEntity`

```json
{
  "message": "完整回复内容",
  "botMsgId": "msg_001",
  "runId": null,
  "chatSessionId": 2001,
  "sessionCode": "cs_001",
  "sourceType": "chat",
  "sources": null,
  "sceneType": "chat"
}
```

2. Agent 失败兜底路径：`AgentFinishResponse`

```json
{
  "message": "任务执行失败：未知错误",
  "botMsgId": "agent_001",
  "runId": 1001,
  "status": "failed",
  "sources": null,
  "chatSessionId": 2001,
  "sessionCode": "cs_agent_001"
}
```

说明：

- `/chat/doChat`、`/rag/search`、`/internet/search` 当前成功路径都返回 `ChatResponseEntity`，其中 `runId` 通常为 `null`
- `/agent/execute` 当前成功路径的 `finish` 也沿用 `ChatResponseEntity`，其中 `runId` 为当前 Agent 运行主键
- `/agent/execute` 在聊天完成前失败时，才会向前端补发 `AgentFinishResponse`
- `add` 事件当前对应 `ChatStreamChunkResponse`，便于前端恢复 `botMsgId/runId/chatSessionId/sessionCode`
- `AgentFinishResponse` 在成功场景下当前更多用于 Agent run 结果落库与审计，不是前端主 finish 负载

---

#### 5.4 发送单个消息

**接口路径：** `GET /sse/sendMessage`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**请求参数：**

| 参数名  | 类型   | 必填 | 说明     |
| ------- | ------ | ---- | -------- |
| message | string | 是   | 消息内容 |

**行为：**

- 向当前登录会话对应的 SSE 连接发送 `message` 事件

**响应类型：** 纯文本字符串

**响应示例：**

```text
success
```

---

#### 5.5 发送追加消息

**接口路径：** `GET /sse/sendMessageAdd`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**请求参数：**

| 参数名  | 类型   | 必填 | 说明     |
| ------- | ------ | ---- | -------- |
| message | string | 是   | 追加内容 |

**行为：**

- 向当前登录会话对应的 SSE 连接连续发送 10 次 `add` 事件
- 主要用于测试流式追加效果

**响应类型：** 纯文本字符串

**响应示例：**

```text
success
```

---

#### 5.6 群发消息

**接口路径：** `GET /sse/sendMessageAll`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护，但尚未增加额外角色授权控制

**请求参数：**

| 参数名  | 类型   | 必填 | 说明     |
| ------- | ------ | ---- | -------- |
| message | string | 是   | 消息内容 |

**行为：**

- 向全部已连接用户广播 `message` 事件

**响应类型：** 纯文本字符串

**响应示例：**

```text
success
```

---

### 6. 测试模块 `/hello`

#### 6.1 Hello World

**接口路径：** `GET /hello/world`

**响应类型：** 纯文本字符串

**响应示例：**

```text
Hello, World!
```

---

#### 6.2 简单聊天

**接口路径：** `GET /hello/chat`

**请求参数：**

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| msg    | string | 是   | 消息内容 |

**响应类型：** 纯文本字符串

---

#### 6.3 流式响应测试

**接口路径：** `GET /hello/chat/stream/response`

**请求参数：**

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| msg    | string | 是   | 消息内容 |

**响应类型：** `Flux<ChatResponse>`

---

#### 6.4 流式字符串测试

**接口路径：** `GET /hello/chat/stream/str`

**请求参数：**

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| msg    | string | 是   | 消息内容 |

**响应类型：** `Flux<String>`

---

### 7. 训练与身体指标模块 `/training` 与 `/body-metrics`

#### 7.1 记录训练日志

**接口路径：** `POST /training/log`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 基于当前登录用户记录当天训练日志
- 同一用户同一日期重复提交时，会覆盖当天训练主记录并重建动作明细
- 服务端会自动计算 `totalVolume = Σ(sets * reps * weight)`
- 空动作名会先被过滤；若过滤后没有有效动作，会直接报错
- 服务端会自动生成训练摘要，供 `/training/recent` 直接展示

**请求体：**

| 参数名    | 类型                     | 必填 | 说明                        |
| --------- | ------------------------ | ---- | --------------------------- |
| date      | string                   | 是   | 训练日期，格式 `yyyy-MM-dd` |
| exercises | `TrainingExerciseItem[]` | 是   | 训练动作列表                |

`TrainingExerciseItem` 字段：

| 参数名 | 类型   | 必填 | 说明                    |
| ------ | ------ | ---- | ----------------------- |
| name   | string | 是   | 动作名称                |
| sets   | number | 否   | 组数，缺省按 1 处理     |
| reps   | number | 否   | 每组次数，缺省按 1 处理 |
| weight | number | 否   | 重量 kg，缺省按 0 处理  |

**请求示例：**

```json
{
  "date": "2026-03-25",
  "exercises": [
    {
      "name": "卧推",
      "sets": 4,
      "reps": 10,
      "weight": 60
    },
    {
      "name": "深蹲",
      "sets": 5,
      "reps": 5,
      "weight": 100
    }
  ]
}
```

**响应类型：** `LeeResult`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": null
}
```

**额外说明：**

- 当前训练摘要最多取前 3 个有效动作进行拼接
- 动作详情会按请求顺序写入 `orderNum`

---

#### 7.2 查询近期训练摘要

**接口路径：** `GET /training/recent`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**请求参数：**

| 参数名 | 类型   | 必填 | 说明                          |
| ------ | ------ | ---- | ----------------------------- |
| limit  | number | 否   | 返回条数，缺省 5，当前最大 20 |

**请求示例：**

```text
GET /training/recent?limit=5
```

**响应类型：** `LeeResult<List<DateSummaryItem>>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": [
    {
      "date": "2026-03-25",
      "summary": "卧推 4组x10次，60kg；深蹲 5组x5次，100kg"
    }
  ]
}
```

---

#### 7.3 记录身体指标

**接口路径：** `POST /body-metrics/log`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**功能描述：**

- 基于当前登录用户记录当天身体指标
- 同一用户同一日期重复提交时，会覆盖当天记录
- `weight` 与 `bodyFat` 至少要填写一项，否则直接报错
- 服务端会基于 `weight/bodyFat/sleep/fatigue` 生成摘要，供 `/body-metrics/recent` 直接展示

**请求体：**

| 参数名  | 类型   | 必填 | 说明                        |
| ------- | ------ | ---- | --------------------------- |
| date    | string | 是   | 记录日期，格式 `yyyy-MM-dd` |
| weight  | number | 否   | 体重 kg                     |
| bodyFat | number | 否   | 体脂 %                      |
| sleep   | number | 否   | 睡眠时长 h                  |
| fatigue | string | 否   | 疲劳度，例如 `低/中/高`     |
| note    | string | 否   | 备注                        |

**请求示例：**

```json
{
  "date": "2026-03-25",
  "weight": 72.5,
  "bodyFat": 15.2,
  "sleep": 7.5,
  "fatigue": "中",
  "note": "腿部轻微酸痛"
}
```

**响应类型：** `LeeResult`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": null
}
```

---

#### 7.4 查询近期身体指标摘要

**接口路径：** `GET /body-metrics/recent`

**鉴权要求：**

- 需要 `headerUserToken`
- 当前接口受统一 token 鉴权保护

**请求参数：**

| 参数名 | 类型   | 必填 | 说明                          |
| ------ | ------ | ---- | ----------------------------- |
| limit  | number | 否   | 返回条数，缺省 5，当前最大 20 |

**请求示例：**

```text
GET /body-metrics/recent?limit=5
```

**响应类型：** `LeeResult<List<DateSummaryItem>>`

**成功响应示例：**

```json
{
  "status": 200,
  "msg": "OK",
  "data": [
    {
      "date": "2026-03-25",
      "summary": "体重 72.5kg，体脂 15.2%，睡眠 7.5小时，疲劳度 中"
    }
  ]
}
```

---

## 数据结构定义

当前聊天模式的两个核心语义如下：

- `sceneType`：主模式，当前只区分 `chat` / `agent`
- `sourceType`：本轮来源类型，当前只区分 `chat` / `rag` / `internet`

### 1. ChatEntity

聊天请求实体：

```java
public class ChatEntity {
    private String currentUserName;
    private String message;
    private String botMsgId;
    private String sessionCode;
    private String clientRequestId;
    private Boolean ragEnabled;
    private Boolean internetEnabled;
}
```

| 字段            | 类型    | 说明                                                                                                                                        |
| --------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| currentUserName | string  | 兼容旧请求体字段，不作为可信身份来源；在 `/chat/doChat`、`/agent/execute`、`/rag/search`、`/internet/search` 中都会被服务端当前登录用户覆盖 |
| message         | string  | 用户消息内容                                                                                                                                |
| botMsgId        | string  | 前端机器人消息 ID；用于流式消息归并，在 `/agent/execute` 中还是幂等关键字段                                                                 |
| sessionCode     | string  | 前端会话编码；用于续聊时复用当前会话                                                                                                        |
| clientRequestId | string  | 客户端请求标识；当前主要用于透传，后端未参与业务判断                                                                                        |
| ragEnabled      | boolean | 是否开启知识库增强；开启后当前轮 `sourceType = rag`                                                                                         |
| internetEnabled | boolean | 是否开启联网补充；开启后当前轮 `sourceType = internet`                                                                                      |

说明：

- 当前主聊天链路不支持 `ragEnabled=true` 且 `internetEnabled=true` 同时开启；服务端会直接抛错
- `/rag/search` 与 `/internet/search` 属于手动入口，本身不依赖这两个布尔开关

### 2. ChatResponseEntity

聊天成功路径返回结构：

```java
public class ChatResponseEntity {
    private String message;
    private String botMsgId;
    private Long runId;
    private Long chatSessionId;
    private String sessionCode;
    private String sourceType;
    private Object sources;
    private String sceneType;
}
```

| 字段          | 类型   | 说明                                                                            |
| ------------- | ------ | ------------------------------------------------------------------------------- |
| message       | string | 完整回复内容                                                                    |
| botMsgId      | string | 对应的机器人消息 ID                                                             |
| runId         | number | Agent 成功路径下的运行主记录 ID；普通 `chat/rag/internet` 成功路径通常为 `null` |
| chatSessionId | number | 当前会话主键                                                                    |
| sessionCode   | string | 当前会话编码                                                                    |
| sourceType    | string | 当前轮来源类型；当前主链路只使用 `chat`、`rag`、`internet`                      |
| sources       | object | 当前轮来源信息；普通聊天通常为 `null`，RAG / 联网问答通常为来源列表             |
| sceneType     | string | 当前主模式；当前只区分 `chat` 与 `agent`                                        |

### 3. AgentExecuteAckResponse

`/agent/execute` HTTP ack 数据结构：

```java
public class AgentExecuteAckResponse {
    private Long runId;
    private Long chatSessionId;
    private String sessionCode;
    private String botMsgId;
    private String status;
    private Boolean duplicate;
}
```

| 字段          | 类型    | 说明                                      |
| ------------- | ------- | ----------------------------------------- |
| runId         | number  | Agent 运行主记录 ID                       |
| chatSessionId | number  | 对应聊天会话 ID                           |
| sessionCode   | string  | 会话编码                                  |
| botMsgId      | string  | 前端机器人消息 ID                         |
| status        | string  | 当前运行状态，受理成功时通常为 `pending`  |
| duplicate     | boolean | 是否命中幂等；`true` 表示返回的是已有任务 |

### 4. AgentStepEvent

`custom_event` 的步骤事件数据结构：

```java
public class AgentStepEvent {
    private Long runId;
    private Integer stepNo;
    private String stepName;
    private String stepStatus;
    private String message;
}
```

| 字段       | 类型   | 说明                                        |
| ---------- | ------ | ------------------------------------------- |
| runId      | number | Agent 运行主记录 ID                         |
| stepNo     | number | 步骤序号                                    |
| stepName   | string | 步骤名称                                    |
| stepStatus | string | 步骤状态，当前使用 `running/success/failed` |
| message    | string | 当前步骤提示文案                            |

### 5. AgentFinishResponse

Agent 失败兜底 `finish` 事件，以及 run 结果落库使用的数据结构：

```java
public class AgentFinishResponse {
    private String message;
    private String botMsgId;
    private Long runId;
    private String status;
    private Object sources;
    private Long chatSessionId;
    private String sessionCode;
}
```

| 字段          | 类型   | 说明                                |
| ------------- | ------ | ----------------------------------- |
| message       | string | 最终返回文案或失败信息              |
| botMsgId      | string | 前端机器人消息 ID                   |
| runId         | number | Agent 运行主记录 ID                 |
| status        | string | 当前状态，例如 `success` / `failed` |
| sources       | object | 结果来源；失败时通常为 `null`       |
| chatSessionId | number | 对应聊天会话 ID                     |
| sessionCode   | string | 对应会话编码                        |

### 5.1 ChatStreamChunkResponse

当前 `add` 事件的数据结构：

```java
public class ChatStreamChunkResponse {
    private String contentChunk;
    private String botMsgId;
    private Long runId;
    private Long chatSessionId;
    private String sessionCode;
    private String sceneType;
    private String sourceType;
}
```

| 字段          | 类型   | 说明                                                                |
| ------------- | ------ | ------------------------------------------------------------------- |
| contentChunk  | string | 当前这一次流式追加的文本分片                                        |
| botMsgId      | string | 对应的机器人消息 ID                                                 |
| runId         | number | Agent 运行主记录 ID；普通 `chat/rag/internet` 成功路径通常为 `null` |
| chatSessionId | number | 当前会话主键                                                        |
| sessionCode   | string | 当前会话编码                                                        |
| sceneType     | string | 当前主模式；当前只区分 `chat` 与 `agent`                            |
| sourceType    | string | 当前轮来源类型；当前只区分 `chat`、`rag`、`internet`                |

### 5.2 AgentRunListItemResponse

`GET /agent/runs` 返回的单条运行摘要结构：

```java
public class AgentRunListItemResponse {
    private Long runId;
    private Long chatSessionId;
    private String sessionCode;
    private String botMsgId;
    private String requestText;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
```

| 字段          | 类型   | 说明                            |
| ------------- | ------ | ------------------------------- |
| runId         | number | Agent 运行主记录 ID             |
| chatSessionId | number | 对应聊天会话 ID                 |
| sessionCode   | string | 会话编码                        |
| botMsgId      | string | 机器人消息 ID                   |
| requestText   | string | 本次请求原文                    |
| status        | string | 运行状态                        |
| errorMessage  | string | 失败原因；成功时通常为 `null`   |
| createdAt     | string | run 创建时间                    |
| startedAt     | string | run 开始执行时间                |
| finishedAt    | string | run 完成时间；未完成时为 `null` |

### 5.3 AgentRunStepResponse

`GET /agent/runs/{runId}` 中单条步骤结构：

```java
public class AgentRunStepResponse {
    private Integer stepNo;
    private String stepName;
    private String stepStatus;
    private String toolName;
    private String inputJson;
    private String outputJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
```

| 字段         | 类型   | 说明                        |
| ------------ | ------ | --------------------------- |
| stepNo       | number | 步骤序号                    |
| stepName     | string | 步骤名称                    |
| stepStatus   | string | 步骤状态                    |
| toolName     | string | 工具名称；当前通常为 `null` |
| inputJson    | string | 步骤输入 JSON               |
| outputJson   | string | 步骤输出 JSON               |
| errorMessage | string | 步骤错误信息                |
| startedAt    | string | 步骤开始时间                |
| finishedAt   | string | 步骤完成时间                |

### 5.4 AgentRunDetailResponse

`GET /agent/runs/{runId}` 返回的运行详情结构：

```java
public class AgentRunDetailResponse {
    private Long runId;
    private Long chatSessionId;
    private String sessionCode;
    private String botMsgId;
    private String requestText;
    private String status;
    private String resultJson;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private List<AgentRunStepResponse> steps;
}
```

| 字段          | 类型                     | 说明                            |
| ------------- | ------------------------ | ------------------------------- |
| runId         | number                   | Agent 运行主记录 ID             |
| chatSessionId | number                   | 对应聊天会话 ID                 |
| sessionCode   | string                   | 会话编码                        |
| botMsgId      | string                   | 机器人消息 ID                   |
| requestText   | string                   | 本次请求原文                    |
| status        | string                   | 运行状态                        |
| resultJson    | string                   | 最终结果 JSON 字符串            |
| errorMessage  | string                   | 失败原因；成功时通常为 `null`   |
| createdAt     | string                   | run 创建时间                    |
| startedAt     | string                   | run 开始执行时间                |
| finishedAt    | string                   | run 完成时间；未完成时为 `null` |
| steps         | `AgentRunStepResponse[]` | 固定步骤列表，按 `stepNo` 正序  |

### 6. ChatRecordsResponse

聊天历史查询响应：

```java
public class ChatRecordsResponse {
    private Long userId;
    private Integer totalSessions;
    private List<ChatSessionRecordItem> sessions;
}
```

| 字段          | 类型                      | 说明                              |
| ------------- | ------------------------- | --------------------------------- |
| userId        | number                    | 当前登录用户主键                  |
| totalSessions | number                    | 本次返回的会话条数                |
| sessions      | `ChatSessionRecordItem[]` | 会话列表，按 `updatedAt` 倒序返回 |

### 7. ChatSessionRecordItem

单个会话的历史记录项：

```java
public class ChatSessionRecordItem {
    private Long sessionId;
    private String sessionCode;
    private String sceneType;
    private String title;
    private String lastBotMsgId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ChatRecordItem> messages;
}
```

| 字段         | 类型               | 说明                                |
| ------------ | ------------------ | ----------------------------------- |
| sessionId    | number             | 会话主键                            |
| sessionCode  | string             | 会话编码                            |
| sceneType    | string             | 主模式，当前只区分 `chat` / `agent` |
| title        | string             | 会话标题                            |
| lastBotMsgId | string             | 最近一条机器人消息 ID               |
| createdAt    | string             | 会话创建时间                        |
| updatedAt    | string             | 会话最近更新时间                    |
| messages     | `ChatRecordItem[]` | 会话内消息列表，按 `seqNo` 正序返回 |

### 8. ChatRecordItem

单条聊天消息记录项：

```java
public class ChatRecordItem {
    private Long sessionId;
    private String sessionCode;
    private String sceneType;
    private Long messageId;
    private Integer seqNo;
    private String role;
    private String messageType;
    private String sourceType;
    private String content;
    private String botMsgId;
    private String sourcesJson;
    private LocalDateTime createdAt;
}
```

| 字段        | 类型   | 说明                                                       |
| ----------- | ------ | ---------------------------------------------------------- |
| sessionId   | number | 所属会话主键                                               |
| sessionCode | string | 所属会话编码                                               |
| sceneType   | string | 所属主模式                                                 |
| messageId   | number | 消息主键                                                   |
| seqNo       | number | 会话内消息顺序号                                           |
| role        | string | 消息角色，当前主要为 `user` / `assistant`                  |
| messageType | string | 当前主要为 `text`                                          |
| sourceType  | string | 本轮来源类型，当前主链路只使用 `chat` / `rag` / `internet` |
| content     | string | 消息内容                                                   |
| botMsgId    | string | assistant 消息对应的机器人消息 ID                          |
| sourcesJson | string | assistant 来源信息的原始 JSON 字符串                       |
| createdAt   | string | 消息创建时间                                               |

### 9. RagConfigResponse

手动 RAG 配置响应：

```java
public class RagConfigResponse {
    private Integer defaultTopK;
    private Integer maxTopK;
    private Integer filterScanLimit;
    private Boolean userIsolationEnabled;
    private String isolationStrategy;
}
```

| 字段                 | 类型    | 说明                                                                                               |
| -------------------- | ------- | -------------------------------------------------------------------------------------------------- |
| defaultTopK          | number  | 默认召回条数                                                                                       |
| maxTopK              | number  | 允许的最大召回条数                                                                                 |
| filterScanLimit      | number  | 历史兼容字段；当前主检索策略已改为 `filterExpression` 用户隔离，前端可读取但不应再按旧扫描语义理解 |
| userIsolationEnabled | boolean | 是否开启用户隔离                                                                                   |
| isolationStrategy    | string  | 当前隔离策略，当前实现为 `vectorstore_filterExpression_userId`                                     |

### 10. RagDocumentItem

用户上传的 RAG 文档列表项：

```java
public class RagDocumentItem {
    private Long id;
    private String fileName;
    private Integer sourceCount;
    private Integer chunkCount;
    private String status;
    private LocalDateTime createdAt;
}
```

| 字段        | 类型   | 说明                             |
| ----------- | ------ | -------------------------------- |
| id          | number | 文档主键                         |
| fileName    | string | 文件名                           |
| sourceCount | number | 原始文档条目数                   |
| chunkCount  | number | 切分后片段数                     |
| status      | string | 当前状态，当前实现主要为 `READY` |
| createdAt   | string | 创建时间                         |

### 11. RagBenchmarkQuestionRequest

RAG 基准评测单题请求：

```java
public class RagBenchmarkQuestionRequest {
    private String question;
    private String expectedFileName;
}
```

| 字段             | 类型   | 说明             |
| ---------------- | ------ | ---------------- |
| question         | string | 问题文本         |
| expectedFileName | string | 预期命中的文件名 |

### 12. RagBenchmarkEvaluateRequest

RAG 基准评测请求：

```java
public class RagBenchmarkEvaluateRequest {
    private String datasetName;
    private Integer topK;
    private List<RagBenchmarkQuestionRequest> questions;
}
```

| 字段        | 类型                            | 说明                                        |
| ----------- | ------------------------------- | ------------------------------------------- |
| datasetName | string                          | 数据集名称，可为空                          |
| topK        | number                          | 期望召回条数，可为空                        |
| questions   | `RagBenchmarkQuestionRequest[]` | 评测题目列表，至少 1 条，当前最多支持 20 条 |

### 13. RagBenchmarkQuestionResultResponse

RAG 基准评测单题结果：

```java
public class RagBenchmarkQuestionResultResponse {
    private Integer index;
    private String question;
    private String expectedFileName;
    private Boolean hit;
    private List<String> retrievedFileNames;
    private String topHitPreview;
}
```

| 字段               | 类型     | 说明                     |
| ------------------ | -------- | ------------------------ |
| index              | number   | 题目序号，从 1 开始      |
| question           | string   | 问题文本                 |
| expectedFileName   | string   | 预期命中的文件名         |
| hit                | boolean  | 是否命中                 |
| retrievedFileNames | string[] | 本次检索返回的文件名列表 |
| topHitPreview      | string   | 第一条检索结果的文本预览 |

### 14. RagBenchmarkEvaluateResponse

RAG 基准评测响应：

```java
public class RagBenchmarkEvaluateResponse {
    private Long runId;
    private String datasetName;
    private Integer questionCount;
    private Integer topK;
    private String status;
    private Integer hitCount;
    private Double hitRate;
    private Boolean userIsolationEnabled;
    private String isolationStrategy;
    private List<RagBenchmarkQuestionResultResponse> results;
}
```

| 字段                 | 类型                                   | 说明                                       |
| -------------------- | -------------------------------------- | ------------------------------------------ |
| runId                | number                                 | 本次评测记录主键                           |
| datasetName          | string                                 | 数据集名称                                 |
| questionCount        | number                                 | 题目总数                                   |
| topK                 | number                                 | 本次评测实际执行的召回条数                 |
| status               | string                                 | 当前结果状态，当前成功路径固定为 `success` |
| hitCount             | number                                 | 命中题数                                   |
| hitRate              | number                                 | 命中率，当前使用 `0~1` 小数表示            |
| userIsolationEnabled | boolean                                | 是否开启用户隔离                           |
| isolationStrategy    | string                                 | 当前隔离策略                               |
| results              | `RagBenchmarkQuestionResultResponse[]` | 单题评测结果列表                           |

### 15. SearchResult

联网搜索结果实体：

```java
public class SearchResult {
    private String title;
    private String url;
    private String content;
    private double score;
}
```

| 字段    | 类型   | 说明         |
| ------- | ------ | ------------ |
| title   | string | 搜索结果标题 |
| url     | string | 来源链接     |
| content | string | 搜索摘要     |
| score   | number | 相关性分数   |

### 16. TrainingExerciseItem

训练动作项实体：

```java
public class TrainingExerciseItem {
    private String name;
    private Integer sets;
    private Integer reps;
    private BigDecimal weight;
}
```

| 字段   | 类型   | 说明                              |
| ------ | ------ | --------------------------------- |
| name   | string | 动作名称                          |
| sets   | number | 组数，当前服务端缺省按 1 处理     |
| reps   | number | 每组次数，当前服务端缺省按 1 处理 |
| weight | number | 重量 kg，当前服务端缺省按 0 处理  |

### 17. TrainingLogRequest

训练日志请求实体：

```java
public class TrainingLogRequest {
    private String date;
    private List<TrainingExerciseItem> exercises;
}
```

| 字段      | 类型                     | 说明                        |
| --------- | ------------------------ | --------------------------- |
| date      | string                   | 训练日期，格式 `yyyy-MM-dd` |
| exercises | `TrainingExerciseItem[]` | 训练动作列表                |

### 18. BodyMetricsLogRequest

身体指标请求实体：

```java
public class BodyMetricsLogRequest {
    private String date;
    private BigDecimal weight;
    private BigDecimal bodyFat;
    private BigDecimal sleep;
    private String fatigue;
    private String note;
}
```

| 字段    | 类型   | 说明                            |
| ------- | ------ | ------------------------------- |
| date    | string | 记录日期，格式 `yyyy-MM-dd`     |
| weight  | number | 体重 kg                         |
| bodyFat | number | 体脂 %                          |
| sleep   | number | 睡眠时长 h                      |
| fatigue | string | 疲劳度，例如 `低` / `中` / `高` |
| note    | string | 备注                            |

### 19. DateSummaryItem

近期摘要项实体：

```java
public class DateSummaryItem {
    private String date;
    private String summary;
}
```

| 字段    | 类型   | 说明             |
| ------- | ------ | ---------------- |
| date    | string | 日期字符串       |
| summary | string | 列表摘要展示内容 |

说明：

- `/training/recent` 与 `/body-metrics/recent` 当前都返回 `LeeResult<List<DateSummaryItem>>`

### 20. LeeResult

当前项目大量接口使用的统一封装：

```java
public class LeeResult {
    private Integer status;
    private String msg;
    private Object data;
}
```

| 字段   | 类型   | 说明         |
| ------ | ------ | ------------ |
| status | number | 业务状态码   |
| msg    | string | 提示消息     |
| data   | object | 具体响应数据 |

### 21. SSEMsgType

当前枚举定义如下：

```java
public enum SSEMsgType {
    MESSAGE("message", "单词发送的普通类型消息"),
    ADD("add", "消息追加，适用于流式stream推送"),
    FINISH("finish", "消息完成"),
    CUSTOM_EVENT("custom_event", "自定义事件"),
    DONE("done", "消息完成");
}
```

说明：

- 当前真实业务发送的是 `message`、`add`、`finish`、`custom_event`
- `done` 目前仍是预留枚举值，未在业务代码中实际发送
- 当前前端主聊天链路主要依赖 `add`、`finish`、`custom_event`

---

## 前端占位接口与后续规划

本节内容不是当前后端正式契约，只用于说明：

- 前端当前已经预留了哪些接口
- 后端未来扩展时建议如何落地

### 1. 当前前端占位接口

当前暂无仍未落地的前端占位接口。

说明：

- `/rag/benchmark/evaluate` 已在当前后端落地，并已升级为正式契约
- 本节当前只保留仍未落地的前端占位接口

### 2. 规划接口维护原则

建议把未来接口分为两层：

#### 当前正式契约

- 只记录后端已经实现并可直接联调的接口

#### 后续扩展规划

- 只记录“推荐命名”和“功能意图”
- 不要提前写成仿佛已经可用的正式契约
- 等后端控制器和 DTO 落地后，再升级为正式接口文档

### 3. 推荐的未来扩展方向

| 业务域             | 推荐根路径       | 推荐说明                                                |
| ------------------ | ---------------- | ------------------------------------------------------- |
| RAG 基准评估       | `/rag/benchmark` | 例如 `/rag/benchmark/evaluate`                          |
| Agent 运行筛选扩展 | `/agent/runs`    | 例如为已实现的 `/agent/runs` 增加按时间、状态、会话过滤 |
| 会话详情拆分       | `/chat/sessions` | 例如 `/chat/sessions/{sessionId}/records`               |

---

## 接口命名建议

### `/chat` 与 `/ollama` 的取舍结论

**结论：对外正式业务接口应统一使用 `/chat`，不建议使用 `/ollama` 作为公开 API 根路径。**

原因如下：

1. `/chat` 是业务语义

   - 表达的是“聊天能力”
   - 面向前端和业务方更稳定

2. `/ollama` 是技术实现语义

   - 它代表底层模型供应商或推理实现
   - 一旦未来从 Ollama 切换到其他模型服务，这个命名会立刻失去准确性

3. 当前后端真实实现已经采用 `/chat/*`

   - 当前后端已有接口：`/chat/chatTest`、`/chat/doChat`、`/chat/records`
   - 因此继续沿着 `/chat` 扩展，最符合现状和可维护性

### 当前文档中的处理方式

- `/chat/*`：作为当前正式业务命名空间
- `/ollama/*`：仅视为前端历史占位或遗留调用，不写入正式接口主清单

### 如果未来要进一步规范“聊天记录/会话记录”能力

推荐优先考虑以下命名之一：

```text
GET /chat/records
GET /chat/sessions/{sessionId}/records
```

说明：

- 当前后端已落地 `GET /chat/records`
- 当前 `who` 参数仅用于兼容旧前端调用，不承担真实身份语义
- 后续如果要进一步细化会话模型，建议从兼容参数演进到 `sessionId` 或 RESTful 会话路径

---

## 更新日志

| 版本 | 日期       | 更新内容                                                                                                                                                                                                                                |
| ---- | ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| v1.0 | 2024       | 初始版本                                                                                                                                                                                                                                |
| v2.0 | 2026-03-23 | 以后端真实实现为准重写文档；移除把前端占位接口误写为当前契约的内容；明确 `/chat` 优于 `/ollama` 的命名建议                                                                                                                              |
| v2.1 | 2026-03-24 | 补充 `/user/code`、`/user/login`、`/user/logout`、`/user/sse-ticket` 契约；更新统一 token 鉴权与 SSE ticket 建连规则；删除旧 `userId/currentUserName` 身份说明                                                                          |
| v2.2 | 2026-03-25 | 将 `/training/log`、`/training/recent`、`/body-metrics/log`、`/body-metrics/recent` 升级为正式契约；补充训练与身体指标数据结构；同步统一 token 鉴权范围说明                                                                             |
| v2.3 | 2026-03-28 | 将 `/agent/execute` 升级为正式契约；补充 Agent ack/步骤/失败数据结构；同步 `ChatEntity` 新字段、`custom_event` 实际发送情况、RAG 用户隔离入口与 `/agent/**` 鉴权范围说明                                                                |
| v2.4 | 2026-03-30 | 将 `/chat/records`、`/rag/docs`、`/rag/config` 升级为正式契约；同步二维模式下 `ragEnabled/internetEnabled`、`ChatResponseEntity` / `AgentFinishResponse` / 聊天历史结构，以及聊天与 Agent 会话归并说明                                  |
| v2.5 | 2026-03-30 | 修复 `/internet/search`、SSE `finish`、训练/身体指标与数据结构章节的文档错位与 Markdown 破损；清理前端占位接口误判，并补充更准确的命名与扩展说明                                                                                        |
| v2.6 | 2026-03-31 | 将 `/agent/runs`、`/agent/runs/{runId}` 升级为正式契约；补充 `AgentRunListItemResponse`、`AgentRunDetailResponse`、`AgentRunStepResponse`、`ChatStreamChunkResponse` 结构；同步 `add/finish` 事件与 `ChatResponseEntity.runId` 文档说明 |
| v2.7 | 2026-03-31 | 将 `/rag/benchmark/evaluate` 升级为正式契约；补充 `RagBenchmarkQuestionRequest`、`RagBenchmarkEvaluateRequest`、`RagBenchmarkQuestionResultResponse`、`RagBenchmarkEvaluateResponse` 结构；同步修正 RAG 配置示例并清理前端占位接口描述  |
| v2.8 | 2026-03-31 | 根据当前代码实现修正文档细节：将 `isolationStrategy` 更新为 `vectorstore_filterExpression_userId`；补充 `filterScanLimit` 为历史兼容字段说明；同步修正 `/rag/docs` 中 `status` 示例值为 `READY`                                         |
