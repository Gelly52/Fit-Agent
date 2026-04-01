# RAG检索增强生成实现流程

## 1. 当前实现范围

- 当前项目中的 RAG 能力，围绕“当前登录用户的手动知识库”展开。
- 当前正式入口包括：
  - `POST /rag/uploadRagDoc`
  - `GET /rag/doSearch`
  - `POST /rag/search`
  - `GET /rag/docs`
  - `GET /rag/config`
  - `POST /rag/config`
  - `POST /rag/benchmark/evaluate`
- 当前 `DocumentService` 负责上传、检索、文档列表与配置读取。
- 当前 benchmark 只做“检索命中率评测”，不做答案质量评测。
- 当前用户隔离依赖 `metadata.userId`，文件命中判断依赖 `metadata.fileName`。
- 当前 `getRagConfig()` 返回的是代码内置配置，并未从 `t_rag_config` 表动态读取。

## 2. 文档上传与索引建立

- 用户上传入口是 `POST /rag/uploadRagDoc`。
- `RagController.uploadRagDoc()` 会先从登录上下文读取当前 `userId`。
- 控制器随后调用 `DocumentService.loadText(resource, fileName, userId)`。
- `DocumentServiceImpl.loadText()` 当前会按以下顺序执行：
  1. 校验 `userId` 非空；
  2. 用 `TextReader` 读取文本；
  3. 在原始文档和切分文档的 `metadata` 中写入：
     - `fileName`
     - `userId`
  4. 使用 `TokenTextSplitter` 做分片；
  5. 调用 `RedisVectorStore.add(splitDocuments)` 写入向量库；
  6. 向 `t_rag_document` 插入一条文档索引记录；
  7. 返回原始 `Document` 列表。
- 当前 `t_rag_document` 实际主要落库字段包括：
  - `user_id`
  - `file_name`
  - `source_count`
  - `chunk_count`
  - `vector_status`
- 当前代码写入的文档状态是 `READY`。
- 前端当前通常限制上传 `.txt` 且不超过 10MB，但后端当前没有做显式扩展名与大小强校验，因此这仍属于前端限制，不是后端硬约束。

## 3. 检索入口与用户隔离

- 手动检索入口是 `GET /rag/doSearch`。
- 当前控制器固定把 `topK` 传为 `4`。
- 真实检索逻辑在 `DocumentServiceImpl.doSearch()` 中完成。
- 当前服务层执行过程如下：
  1. 校验 `userId` 非空；
  2. 调用 `normalizeTopK(topK)` 规范化请求值；
  3. 计算扫描量：`scanTopK = min(max(safeTopK * 5, safeTopK), FILTER_SCAN_LIMIT)`；
  4. 调用 `RedisVectorStore.similaritySearch(...)` 做相似度检索；
  5. 对返回结果按 `metadata.userId` 做内存过滤；
  6. 截断到 `safeTopK` 后返回。
- 当前默认配置为：
  - `defaultTopK = 4`
  - `maxTopK = 10`
  - `filterScanLimit = 50`
  - `userIsolationEnabled = true`
  - `isolationStrategy = scan_then_filter_by_metadata_userId`
- 当前不是直接依赖向量库原生 filter，而是采用“扩大召回后，在 Java 内存中按 `metadata.userId` 过滤”的保守实现。

## 4. RAG 增强生成入口

- 手动 RAG 问答入口是 `POST /rag/search`。
- `RagController.search()` 当前执行过程如下：
  1. 从登录态读取 `AuthenticatedUserContext`；
  2. 对 `chatEntity.getMessage()` 先执行 `documentService.doSearch(..., 4)`；
  3. 把检索结果传给 `chatService.doChatRagSearch(...)`；
  4. 用当前登录用户 `userKey` 覆盖请求体中的 `currentUserName`；
  5. 返回 `LeeResult<ChatResponseEntity>`；
  6. 同时继续通过 SSE 推送 `add` / `finish` 事件。
- 当前手动调试可以直接走 `/rag/search`。
- 当前主聊天链路若要带知识库增强，更推荐通过主入口开关接入，具体联调契约可继续参考 `docs/API_文档.md`。

## 5. 文档列表与配置接口

### 5.1 文档列表

- 当前入口是 `GET /rag/docs`。
- 控制器会读取当前登录用户 `userId`。
- 服务层会查询 `t_rag_document` 中属于当前用户的数据，并按 `created_at` 倒序返回。
- 当前返回结构是 `RagDocumentItem`，核心字段包括：
  - `id`
  - `fileName`
  - `sourceCount`
  - `chunkCount`
  - `status`
  - `createdAt`

### 5.2 配置读取

- 当前入口包括：
  - `GET /rag/config`
  - `POST /rag/config`
- `POST /rag/config` 只是兼容旧调用方式。
- 当前两者返回内容完全一致。
- 当前配置并不是从数据库实时读取，而是直接来自 `DocumentServiceImpl.getRagConfig()` 中的内置常量。
- 也就是说，虽然数据库里已经存在 `t_rag_config` 表，但当前 `mcp-client` 代码还没有真正读取它。

## 6. Benchmark 检索命中率评测

- 当前入口是 `POST /rag/benchmark/evaluate`。
- 当前 benchmark 的目标非常明确：评测当前用户的 RAG 检索是否能把目标文件召回到 topK 中。
- 当前不做：
  - 答案质量判断；
  - LLM 裁判；
  - 异步任务轮询；
  - MRR、NDCG 等复杂指标。

### 6.1 请求结构

- 当前请求 DTO 是 `RagBenchmarkEvaluateRequest`，字段包括：
  - `datasetName`
  - `topK`
  - `questions`
- 单题结构 `RagBenchmarkQuestionRequest` 字段包括：
  - `question`
  - `expectedFileName`

### 6.2 服务层执行过程

- `RagBenchmarkServiceImpl.evaluate()` 当前按以下顺序执行：
  1. 校验请求体；
  2. 要求 `questions` 至少 1 条，最多 20 条；
  3. 要求每题 `question` 与 `expectedFileName` 都不能为空；
  4. 调用 `documentService.getRagConfig()` 获取当前配置；
  5. 计算本次 `effectiveTopK`；
  6. 向 `t_rag_benchmark_run` 插入一条 `running` 记录；
  7. 逐题调用 `documentService.doSearch(question, userId, effectiveTopK)`；
  8. 提取每题返回结果中的 `metadata.fileName`；
  9. 判断目标文件是否被召回；
  10. 汇总 `hitCount` 与 `hitRate`；
  11. 成功时把 run 更新为 `success`；
  12. 异常时把 run 更新为 `failed`。

### 6.3 命中规则

- 当前命中判定规则非常简单：
  - 只要返回结果中的任意文档满足 `metadata.fileName == expectedFileName`
  - 就判定该题命中。
- 当前是严格文件名匹配。
- 因此 benchmark 数据集中的 `expectedFileName` 最稳妥的来源是 `/rag/docs` 返回结果，不建议手猜文件名。

### 6.4 返回结果

- 当前响应 DTO 是 `RagBenchmarkEvaluateResponse`，核心字段包括：
  - `runId`
  - `datasetName`
  - `questionCount`
  - `topK`
  - `status`
  - `hitCount`
  - `hitRate`
  - `userIsolationEnabled`
  - `isolationStrategy`
  - `results`
- 单题结果 `RagBenchmarkQuestionResultResponse` 核心字段包括：
  - `index`
  - `question`
  - `expectedFileName`
  - `hit`
  - `retrievedFileNames`
  - `topHitPreview`
- 当前结果字段的关键语义：
  - `topK`：本次评测实际执行值；
  - `status`：成功路径固定为 `success`；
  - `hitRate`：当前使用 `0~1` 小数表示；
  - `retrievedFileNames`：按检索结果顺序提取并去重后的文件名列表；
  - `topHitPreview`：第一条检索结果的文本预览，当前最多保留 120 个字符，并会压缩空白字符。

### 6.5 持久化行为

- 当前 benchmark 会写入 `t_rag_benchmark_run`。
- 当前主要写入字段包括：
  - `user_id`
  - `dataset_name`
  - `question_count`
  - `status`
  - `config_snapshot_json`
  - `result_json`
  - `created_at`
  - `updated_at`
- `config_snapshot_json` 当前使用 `JSONUtil.toJsonStr(...)` 保存评测配置快照。
- `result_json` 当前使用 `JSONUtil.toJsonStr(...)` 保存最终结果或失败摘要。

## 7. 涉及数据表的当前定位

### 7.1 `t_rag_document`

- 作用：保存用户上传文档的索引元信息。
- 当前 `mcp-client` 实际主要使用：
  - `id`
  - `user_id`
  - `file_name`
  - `source_count`
  - `chunk_count`
  - `vector_status`
  - `created_at`
  - `updated_at`

### 7.2 `t_rag_config`

- 作用：为未来 RAG 配置动态化预留。
- 当前状态：
  - 表已存在；
  - 当前 `mcp-client` 未读取该表；
  - 当前配置仍来自代码常量。

### 7.3 `t_rag_benchmark_run`

- 作用：保存 benchmark 每次执行的运行记录与结果快照。
- 当前状态流转包括：
  - `running`
  - `success`
  - `failed`

## 8. 前端对接方式

- 前端封装位置是 `Fit-Agent-frontend/src/services/doctorApi.js`。
- 当前已封装的 RAG 相关方法包括：
  - `uploadRagDoc(formData)` -> `/rag/uploadRagDoc`
  - `ragSearch(bo)` -> `/rag/search`
  - `ragConfig(bo)` -> `/rag/config`
  - `benchmarkEvaluate(bo)` -> `/rag/benchmark/evaluate`
  - `getUploadedDocs()` -> `/rag/docs`
- 当前 benchmark 控制台联调示例：

```javascript
window.doctorApi.benchmarkEvaluate({
  datasetName: "smoke-v1",
  topK: 4,
  questions: [
    {
      question: "深蹲的核心动作要点是什么？",
      expectedFileName: "squat-guide.txt"
    },
    {
      question: "蛋白质摄入建议是多少？",
      expectedFileName: "nutrition-notes.txt"
    }
  ]
}).then(console.log).catch(console.error)
```

- 联调前建议：
  1. 先登录；
  2. 先上传文档；
  3. 先调用 `/rag/docs` 确认真实文件名；
  4. benchmark 中的 `expectedFileName` 直接从 `/rag/docs` 复制。

## 9. 关键代码锚点

- 控制器：`Fit-Agent-backend/mcp-client/src/main/java/com/itgeo/controller/RagController.java`
- 文档服务接口：`Fit-Agent-backend/mcp-client/src/main/java/com/itgeo/service/DocumentService.java`
- 文档服务实现：`Fit-Agent-backend/mcp-client/src/main/java/com/itgeo/service/impl/DocumentServiceImpl.java`
- Benchmark 服务接口：`Fit-Agent-backend/mcp-client/src/main/java/com/itgeo/service/RagBenchmarkService.java`
- Benchmark 服务实现：`Fit-Agent-backend/mcp-client/src/main/java/com/itgeo/service/impl/RagBenchmarkServiceImpl.java`
- 文档实体：`Fit-Agent-backend/mcp-client/src/main/java/com/itgeo/pojo/RagDocument.java`
- Benchmark run 实体：`Fit-Agent-backend/mcp-client/src/main/java/com/itgeo/pojo/RagBenchmarkRun.java`
- API 总文档：`docs/API_文档.md`

## 10. 当前实现的关键结论

- 当前 RAG 模块已经具备：
  - 用户文档上传入向量库；
  - 基于当前用户范围的向量检索；
  - 手动 RAG 问答；
  - 文档列表与配置读取；
  - 基于文件命中的 benchmark 评测。
- 当前实现也有明确边界：
  - 配置仍是代码常量，不是数据库配置；
  - 上传接口还没有后端侧的文件类型与大小强校验；
  - benchmark 只评估“检索命中率”，不评估“答案正确率”；
  - `metadata.fileName` 和 `metadata.userId` 是当前实现的关键依赖，后续若修改，必须同步更新实现与文档。 
