# 聊天与 Agent 模式实现流程

## 1. 当前实现范围

- 当前主模式只有 `chat` 与 `agent`。
- `ragEnabled`、`internetEnabled` 只表示本轮增强能力，不再决定主请求入口。
- `sceneType` 表示主会话场景，`sourceType` 表示本轮回答来源类型。
- 当前正式入口包括：
  - `POST /chat/doChat`
  - `POST /agent/execute`
  - `GET /chat/records`
  - `GET /agent/runs`
  - `GET /agent/runs/{runId}`

## 2. 页面初始化与运行恢复

- `SpringAiPage.created()` 会先恢复登录态，再调用 `restoreActiveAgentRun()`。
- `restoreActiveAgentRun()` 会从 `sessionStorage` 读取 `activeAgentRun` 快照；只有 `pending/running` 这类非终态任务才继续恢复。
- 恢复成功后，前端会连续发起两条静默恢复请求：
  - `silentRestoreChatSession(chatSessionId)`：通过 `/chat/records?sessionId=...` 恢复当前会话消息；
  - `silentFetchAgentRunDetail(runId)`：通过 `/agent/runs/{runId}` 恢复 run 状态与步骤状态。
- 如果运行查询接口返回 `404/405/501`，前端会静默降级，不阻断当前页面；后续仅依赖 SSE 驱动。
- 当前存在未完成的 Agent run 时，前端会阻止切换历史会话和新建聊天，避免会话状态被覆盖。
- 刷新恢复可以还原 `runId`、`chatSessionId`、`sessionCode`、`botMsgId`、步骤状态和最终结果；但无法无损恢复刷新前已收到、尚未落库的中途 chunk。

## 3. 发送入口与 ACK 受理

- `doChat()` 发送前必须先 `await ensureSseConnection()`；若 SSE 未连通，前端会直接提示并中止发送。
- 普通模式直接调用 `/chat/doChat`。
- Agent 模式优先调用 `/agent/execute`；只有该请求失败时才 fallback 到 `/chat/doChat`。
- Agent ack 成功后，前端会通过 `applyAgentExecuteAck()` 建立 `activeAgentRun`，写入 `runId/chatSessionId/sessionCode/botMsgId`，并立即再次调用 `silentFetchAgentRunDetail(runId)`。
- 前端发送 Agent 请求时会先用 `buildAgentSteps(message)` 生成本地占位步骤；后续一旦收到 `custom_event` 或 run 详情，应以后端返回为准。

## 4. SSE 事件与前端状态机

### 4.1 `add`

- 当前 `add` 已不是纯文本分片，而是 `ChatStreamChunkResponse`。
- 关键字段包括：`contentChunk`、`botMsgId`、`runId`、`chatSessionId`、`sessionCode`、`sceneType`、`sourceType`。
- 前端通过 `normalizeAddPayload()` 与 `upsertStreamingBotMessage()` 合并流式内容，并同步会话元数据。
- 如果 Agent 场景暂时还没收到步骤事件，前端会用 `updateAgentStepsOnStream()` 临时推进本地占位步骤。

### 4.2 `custom_event`

- 当前 Agent 后端固定维护 5 个步骤，并由 `AgentAsyncServiceImpl.pushStepEvent()` 推送 `AgentStepEvent`。
- 前端通过 `handleAgentCustomEvent()` 与 `applyAgentStepEvent()` 更新步骤 UI。
- 当第 5 步成功结束时，前端会把 `activeAgentRun.status` 标记为 `success`。

### 4.3 `finish`

- 普通聊天与 Agent 成功路径都返回 `ChatResponseEntity`。
- Agent 在聊天完成前失败时，后端才会补发 `AgentFinishResponse`。
- 关键语义：Agent 场景下 `finish` 只表示“回答流结束”，不表示 run 已最终完成。
- 前端在 `applyFinishPayload()` 中，如果当前是 Agent run：
  - 失败时把本地 run 标为 `failed`；
  - 成功时先把本地 run 保持为 `running`，提示“任务结果已返回，正在同步执行状态”；
  - 然后再次静默调用 `/agent/runs/{runId}`，以后端 run/step 状态作为最终依据。

## 5. Run 查询接口的当前定位

- `POST /agent/execute`：只负责受理任务，立即返回 ack，不等待最终回答。
- `GET /agent/runs`：返回当前用户最近的 Agent 运行列表，适合列表页或审计视图；当前 `doctorApi.js` 已封装，但主聊天页恢复路径暂未主动使用。
- `GET /agent/runs/{runId}`：返回指定 run 的详情与步骤，是刷新恢复、finish 后校准、ack 后补查的核心接口。
- `GET /chat/records`：负责恢复当前会话消息，不负责给出 Agent run 的最终状态。

## 6. 后端受理、去重与锁

- `ChatController.doChat()` 仍承担 Agent 请求失败后的 fallback 兜底。
- 为避免重复执行，`ChatController.shouldSkipFallback()` 会按 `userId + botMsgId` 检查：
  - Agent run 是否已经存在；
  - 聊天消息是否已经由 Agent 链路落库。
- `AgentExecuteServiceImpl.execute()` 只负责短事务受理：
  1. 校验 SSE 通道已建立；
  2. 按 `userId + botMsgId` 做幂等；
  3. 创建或复用 `agent` 会话；
  4. 写入 user 消息、assistant 占位、run 与固定步骤；
  5. 对当前登录 `sessionId` 加 session 级 Redis 锁；
  6. 在事务提交后再派发 `AgentAsyncServiceImpl.executeAsync()`。
- Redis 锁的 owner 是 `runId`，不是匿名固定值。
- `AgentAsyncServiceImpl.executeAsync()` 执行期间会通过 `renewLock()` 定时续期，并在 `finally` 中通过 `releaseLock()` 安全释放。
- 锁续期和释放都使用 compare-and-expire / compare-and-delete，只允许当前 owner 操作自己的锁。

## 7. 关键代码锚点

- 前端入口与恢复：`SpringAiPage.created`、`restoreActiveAgentRun`、`silentRestoreChatSession`、`silentFetchAgentRunDetail`、`applyAgentExecuteAck`、`doChat`
- 前端事件处理：`normalizeAddPayload`、`upsertStreamingBotMessage`、`handleAgentCustomEvent`、`applyAgentStepEvent`、`applyFinishPayload`
- 前端接口封装：`doctorApi.getRecords`、`doctorApi.agentExecute`、`doctorApi.getAgentRuns`、`doctorApi.getAgentRunDetail`
- 后端入口与查询：`ChatController.doChat`、`ChatController.shouldSkipFallback`、`AgentController.execute`、`AgentController.listRuns`、`AgentController.getRunDetail`
- 后端受理与异步执行：`AgentExecuteServiceImpl.execute`、`AgentAsyncServiceImpl.executeAsync`、`AgentAsyncServiceImpl.renewLock`、`AgentAsyncServiceImpl.releaseLock`、`ChatServiceImpl.streamAndSend`

## 8. 当前实现的关键结论

- 续聊定位仍以 `sessionCode` 为核心，会话主场景由 `sceneType` 标识。
- 当前主模式只分 `chat / agent`；知识库与联网是增强开关，对应 `sourceType = chat/rag/internet`。
- Agent 最终是否完成，不能只看 SSE `finish`，必须以后端步骤事件或 `/agent/runs/{runId}` 返回为准。
