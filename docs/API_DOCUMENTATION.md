# Fit-Agent 接口文档

> 版本：v2.0  
> 最后更新：2026-03-23  
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

| 模块       | 接口路径                      | 请求方式 | 当前状态 | 返回方式             | 说明             |
| ---------- | ----------------------------- | -------- | -------- | -------------------- | ---------------- |
| 聊天       | `/chat/chatTest`              | POST     | 已实现   | 纯文本字符串         | 测试聊天         |
| 聊天       | `/chat/doChat`                | POST     | 已实现   | `LeeResult` + SSE    | 流式聊天入口     |
| RAG 知识库 | `/rag/uploadRagDoc`           | POST     | 已实现   | `LeeResult`          | 上传知识库文档   |
| RAG 知识库 | `/rag/doSearch`               | GET      | 已实现   | `LeeResult`          | 向量搜索         |
| RAG 知识库 | `/rag/search`                 | POST     | 已实现   | HTTP 空响应体 + SSE  | RAG 增强聊天     |
| 联网搜索   | `/internet/test`              | GET      | 已实现   | `LeeResult`          | 联网搜索测试     |
| 联网搜索   | `/internet/search`            | POST     | 已实现   | HTTP 空响应体 + SSE  | 联网搜索聊天     |
| SSE 通信   | `/sse/connect`                | GET      | 已实现   | `text/event-stream`  | 建立 SSE 连接    |
| SSE 通信   | `/sse/sendMessage`            | GET      | 已实现   | 纯文本字符串         | 发送单条普通消息 |
| SSE 通信   | `/sse/sendMessageAdd`         | GET      | 已实现   | 纯文本字符串         | 发送追加消息测试 |
| SSE 通信   | `/sse/sendMessageAll`         | GET      | 已实现   | 纯文本字符串         | SSE 群发消息     |
| 测试       | `/hello/world`                | GET      | 已实现   | 纯文本字符串         | Hello World      |
| 测试       | `/hello/chat`                 | GET      | 已实现   | 纯文本字符串         | 简单聊天测试     |
| 测试       | `/hello/chat/stream/response` | GET      | 已实现   | `Flux<ChatResponse>` | 流式响应测试     |
| 测试       | `/hello/chat/stream/str`      | GET      | 已实现   | `Flux<String>`       | 流式字符串测试   |

### 当前未实现但前端存在占位调用的接口

以下接口目前只存在于前端 `Fit-Agent-frontend/src/services/doctorApi.js` 中，后端代码未实现，因此不属于当前正式契约：

- `/chat/records?who=...`
- `/agent/execute`
- `/rag/config`
- `/rag/docs`
- `/rag/benchmark/evaluate`
- `/training/log`
- `/training/recent`
- `/body-metrics/log`
- `/body-metrics/recent`

这些接口已在本文档的“前端占位接口与后续规划”章节中单独说明。

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

### 请求头

#### 当前后端实际要求

后端当前没有在控制器中显式读取或校验 `headerUserId`、`headerUserToken`，因此它们不是当前后端的硬性请求头要求。

#### 当前前端约定

前端 HTTP 封装会自动注入以下请求头（如果本地 cookie 中存在相应值）：

| Header            | 当前状态                       | 说明         |
| ----------------- | ------------------------------ | ------------ |
| `headerUserId`    | 前端会发送，后端当前未显式使用 | 预留用户标识 |
| `headerUserToken` | 前端会发送，后端当前未显式使用 | 预留用户令牌 |

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

#### 3. HTTP 触发 + SSE 返回实际业务结果

以下接口的真实业务结果不在 HTTP 响应体中，而是通过 SSE 返回：

- `/chat/doChat`
- `/rag/search`
- `/internet/search`

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

**功能描述：**

- 触发聊天任务
- HTTP 接口只负责发起任务
- 实际生成内容通过 SSE 分块推送

**请求体：**

| 参数名          | 类型   | 必填 | 说明                                                                       |
| --------------- | ------ | ---- | -------------------------------------------------------------------------- |
| currentUserName | string | 是   | 作为 SSE 用户连接标识，必须与 `/sse/connect?userId=...` 中的 `userId` 一致 |
| message         | string | 是   | 用户消息                                                                   |
| botMsgId        | string | 是   | 前端用于归并流式消息的机器人消息 ID                                        |

**请求示例：**

```json
{
  "currentUserName": "user123",
  "message": "帮我制定一个健身计划",
  "botMsgId": "msg_001"
}
```

**HTTP 响应：** `LeeResult`

```json
{
  "status": 200,
  "msg": "OK",
  "data": null
}
```

**SSE 行为：**

- 流式内容通过 `add` 事件不断追加
- 最终完成通过 `finish` 事件返回完整结果
- `finish` 事件的数据是 `ChatResponseEntity` 的 JSON 字符串

**`finish` 事件示例：**

```text
event: finish
data: {"message":"完整回复内容","botMsgId":"msg_001"}
```

---

### 2. RAG 知识库模块 `/rag`

#### 2.1 上传 RAG 文档

**接口路径：** `POST /rag/uploadRagDoc`

**功能描述：**

- 读取上传文件
- 使用 `TextReader` 解析文本
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
        "fileName": "guide.txt"
      }
    }
  ]
}
```

---

#### 2.2 向量搜索

**接口路径：** `GET /rag/doSearch`

**功能描述：**

- 根据 `question` 在 Redis 向量库中做相似度搜索
- 当前实现固定 `topK = 4`

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
        "fileName": "guide.txt"
      }
    }
  ]
}
```

---

#### 2.3 RAG 增强聊天

**接口路径：** `POST /rag/search`

**功能描述：**

- 先根据 `message` 做向量搜索
- 再将检索结果注入提示词
- 最终答案通过 SSE 返回

**请求体：**

| 参数名          | 类型   | 必填 | 说明          |
| --------------- | ------ | ---- | ------------- |
| currentUserName | string | 是   | SSE 用户标识  |
| message         | string | 是   | 用户问题      |
| botMsgId        | string | 是   | 机器人消息 ID |

**请求示例：**

```json
{
  "currentUserName": "user123",
  "message": "深蹲的正确姿势是什么",
  "botMsgId": "msg_002"
}
```

**HTTP 响应：**

- 当前控制器返回 `void`
- 实际表现为 `HTTP 200` 且响应体为空

**SSE 行为：**

- `add`：分块追加内容
- `finish`：返回完整 `ChatResponseEntity` JSON 字符串

---

### 3. 联网搜索模块 `/internet`

#### 3.1 联网搜索测试

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

**响应类型：** `LeeResult`

**响应数据结构：** `SearchResult[]`

| 字段    | 类型   | 说明       |
| ------- | ------ | ---------- |
| title   | string | 标题       |
| url     | string | 链接       |
| content | string | 摘要       |
| score   | number | 相关度分数 |

**响应示例：**

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

#### 3.2 联网搜索聊天

**接口路径：** `POST /internet/search`

**功能描述：**

- 先做联网搜索
- 再用搜索结果构建上下文
- 最终答案通过 SSE 返回

**请求体：**

| 参数名          | 类型   | 必填 | 说明          |
| --------------- | ------ | ---- | ------------- |
| currentUserName | string | 是   | SSE 用户标识  |
| message         | string | 是   | 用户问题      |
| botMsgId        | string | 是   | 机器人消息 ID |

**请求示例：**

```json
{
  "currentUserName": "user123",
  "message": "最新的健身研究有哪些",
  "botMsgId": "msg_003"
}
```

**HTTP 响应：**

- 当前控制器返回 `void`
- 实际表现为 `HTTP 200` 且响应体为空

**SSE 行为：**

- `add`：分块追加内容
- `finish`：返回完整 `ChatResponseEntity` JSON 字符串

---

### 4. SSE 实时通信模块 `/sse`

#### 4.1 建立 SSE 连接

**接口路径：** `GET /sse/connect`

**功能描述：**

- 为指定 `userId` 建立 SSE 连接
- 当前服务端使用 `new SseEmitter(0L)`，即不设置超时时间

**请求参数：**

| 参数名 | 类型   | 必填 | 说明         |
| ------ | ------ | ---- | ------------ |
| userId | string | 是   | 用户唯一标识 |

**请求示例：**

```text
GET /sse/connect?userId=user123
```

**响应类型：** `text/event-stream`

#### 4.2 当前事件模型

需要区分“浏览器原生事件”和“后端主动发送的业务事件”：

| 事件名         | 类型           | 当前状态   | 说明                                         |
| -------------- | -------------- | ---------- | -------------------------------------------- |
| `open`         | 浏览器原生事件 | 当前可见   | 连接建立时浏览器侧触发，不是后端主动命名发送 |
| `error`        | 浏览器原生事件 | 当前可见   | 连接异常/断开时浏览器侧触发                  |
| `message`      | 业务事件       | 已实现     | 普通消息事件                                 |
| `add`          | 业务事件       | 已实现     | 流式追加内容                                 |
| `finish`       | 业务事件       | 已实现     | 返回完整消息结果                             |
| `custom_event` | 预留枚举值     | 未实际发送 | 当前后端没有使用                             |
| `done`         | 预留枚举值     | 未实际发送 | 当前后端没有使用                             |

#### 4.3 `finish` 事件数据结构

`finish` 事件的数据是 `ChatResponseEntity` JSON 字符串：

```json
{
  "message": "完整回复内容",
  "botMsgId": "msg_001"
}
```

---

#### 4.4 发送单个消息

**接口路径：** `GET /sse/sendMessage`

**请求参数：**

| 参数名  | 类型   | 必填 | 说明        |
| ------- | ------ | ---- | ----------- |
| userId  | string | 是   | 目标用户 ID |
| message | string | 是   | 消息内容    |

**行为：**

- 向目标用户发送 `message` 事件

**响应类型：** 纯文本字符串

**响应示例：**

```text
success
```

---

#### 4.5 发送追加消息

**接口路径：** `GET /sse/sendMessageAdd`

**请求参数：**

| 参数名  | 类型   | 必填 | 说明        |
| ------- | ------ | ---- | ----------- |
| userId  | string | 是   | 目标用户 ID |
| message | string | 是   | 追加内容    |

**行为：**

- 当前实现会连续发送 10 次 `add` 事件
- 主要用于测试流式追加效果

**响应类型：** 纯文本字符串

**响应示例：**

```text
success
```

---

#### 4.6 群发消息

**接口路径：** `GET /sse/sendMessageAll`

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

### 5. 测试模块 `/hello`

#### 5.1 Hello World

**接口路径：** `GET /hello/world`

**响应类型：** 纯文本字符串

**响应示例：**

```text
Hello, World!
```

---

#### 5.2 简单聊天

**接口路径：** `GET /hello/chat`

**请求参数：**

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| msg    | string | 是   | 消息内容 |

**响应类型：** 纯文本字符串

---

#### 5.3 流式响应测试

**接口路径：** `GET /hello/chat/stream/response`

**请求参数：**

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| msg    | string | 是   | 消息内容 |

**响应类型：** `Flux<ChatResponse>`

---

#### 5.4 流式字符串测试

**接口路径：** `GET /hello/chat/stream/str`

**请求参数：**

| 参数名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| msg    | string | 是   | 消息内容 |

**响应类型：** `Flux<String>`

---

## 数据结构定义

### 1. ChatEntity

聊天请求实体，当前后端代码如下：

```java
public class ChatEntity {
    private String currentUserName;
    private String message;
    private String botMsgId;
}
```

字段说明：

| 字段            | 类型   | 说明                                         |
| --------------- | ------ | -------------------------------------------- |
| currentUserName | string | 当前用户标识，SSE 场景下会作为 `userId` 使用 |
| message         | string | 用户消息内容                                 |
| botMsgId        | string | 前端机器人消息 ID，用于流式内容归并          |

### 2. ChatResponseEntity

SSE `finish` 事件的数据结构：

```java
public class ChatResponseEntity {
    private String message;
    private String botMsgId;
}
```

字段说明：

| 字段     | 类型   | 说明                |
| -------- | ------ | ------------------- |
| message  | string | 完整回复内容        |
| botMsgId | string | 对应的机器人消息 ID |

说明：旧版文档中的 `fullContent` 字段已废弃，当前真实字段名为 `message`。

### 3. SearchResult

联网搜索结果实体：

```java
public class SearchResult {
    private String title;
    private String url;
    private String content;
    private double score;
}
```

### 4. LeeResult

部分接口使用的统一封装：

```java
public class LeeResult {
    private Integer status;
    private String msg;
    private Object data;
}
```

### 5. SSEMsgType

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

- 当前真实业务发送的是 `message`、`add`、`finish`
- `custom_event`、`done` 目前只是预留枚举值，未在业务代码中实际发送

---

## 前端占位接口与后续规划

本节内容不是当前后端正式契约，只用于说明：

- 前端当前已经预留了哪些接口
- 后端未来扩展时建议如何落地

### 1. 当前前端占位接口

| 前端占位接口              | 当前状态             | 说明                                                         |
| ------------------------- | -------------------- | ------------------------------------------------------------ |
| `/chat/records?who=...`   | 前端占位，后端未实现 | 已按命名规范从历史 `/ollama/*` 占位切换到 `/chat/*` 规划路径 |
| `/agent/execute`          | 前端占位，后端未实现 | 当前前端会尝试调用，但后端暂无控制器                         |
| `/rag/config`             | 前端占位，后端未实现 | 当前后端暂无控制器                                           |
| `/rag/docs`               | 前端占位，后端未实现 | 当前后端暂无控制器                                           |
| `/rag/benchmark/evaluate` | 前端占位，后端未实现 | 当前后端暂无控制器                                           |
| `/training/log`           | 前端占位，后端未实现 | 当前后端暂无控制器                                           |
| `/training/recent`        | 前端占位，后端未实现 | 当前后端暂无控制器                                           |
| `/body-metrics/log`       | 前端占位，后端未实现 | 当前后端暂无控制器                                           |
| `/body-metrics/recent`    | 前端占位，后端未实现 | 当前后端暂无控制器                                           |

### 2. 规划接口维护原则

建议把未来接口分为两层：

#### 当前正式契约

- 只记录后端已经实现并可直接联调的接口

#### 后续扩展规划

- 只记录“推荐命名”和“功能意图”
- 不要提前写成仿佛已经可用的正式契约
- 等后端控制器和 DTO 落地后，再升级为正式接口文档

### 3. 推荐的未来扩展方向

| 业务域   | 推荐根路径      | 推荐说明                                                     |
| -------- | --------------- | ------------------------------------------------------------ |
| 聊天记录 | `/chat`         | 例如 `/chat/records` 或 `/chat/sessions/{sessionId}/records` |
| Agent    | `/agent`        | 保持 `/agent/execute` 方向即可                               |
| RAG 配置 | `/rag`          | 例如 `/rag/config`、`/rag/docs`                              |
| 训练日志 | `/training`     | 例如 `/training/log`、`/training/recent`                     |
| 身体指标 | `/body-metrics` | 例如 `/body-metrics/log`、`/body-metrics/recent`             |

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
   - 当前后端已有接口：`/chat/chatTest`、`/chat/doChat`
   - 因此继续沿着 `/chat` 扩展，最符合现状和可维护性

### 当前文档中的处理方式

- `/chat/*`：作为当前正式业务命名空间
- `/ollama/*`：仅视为前端历史占位或遗留调用，不写入正式接口主清单

### 如果未来要补“聊天记录”能力

推荐优先考虑以下命名之一：

```text
GET /chat/records
GET /chat/sessions/{sessionId}/records
```

当前前端占位已统一切换为：

```text
GET /chat/records?who=...
```

后续如果要进一步规范会话模型，建议再从 `who` 参数演进到 `sessionId` 或 RESTful 会话路径。

---

## 更新日志

| 版本 | 日期       | 更新内容                                                                                                   |
| ---- | ---------- | ---------------------------------------------------------------------------------------------------------- |
| v1.0 | 2024       | 初始版本                                                                                                   |
| v2.0 | 2026-03-23 | 以后端真实实现为准重写文档；移除把前端占位接口误写为当前契约的内容；明确 `/chat` 优于 `/ollama` 的命名建议 |
