# RAG 检索增强生成实现流程

## 1. 当前实现范围

当前项目中的 RAG 能力围绕“当前登录用户的手动知识库”展开，主要入口包括：

- `POST /rag/uploadRagDoc`
- `GET /rag/doSearch`
- `POST /rag/search`
- `GET /rag/docs`
- `GET /rag/config`
- `POST /rag/config`
- `POST /rag/benchmark/evaluate`

当前代码已经支持：

- 可配置分块策略：`token` / `semantic`
- 混合检索链路：向量召回 + 关键词召回 + RRF 融合
- 基于登录用户 `userId` 的检索隔离
- benchmark 检索命中率评测

更详细的语义分块与混合检索说明见：`docs/语义分块与混合检索实现说明.md`

## 2. 当前开发配置

当前活动配置文件：`Fit-Agent-backend/mcp-client/src/main/resources/application-dev.yml`

当前开发配置的关键点如下：

- `rag.embedding.provider = bge-m3-http`
- `rag.vectorstore.index-name = lee-vectorstore-bgem3`
- `rag.vectorstore.prefix = embedding:bgem3:`
- `rag.retrieval.mode = hybrid`
- `rag.retrieval.vector-recall-k = 16`
- `rag.retrieval.keyword-recall-k = 16`
- `rag.retrieval.rrf-k = 60`
- `rag.retrieval.vector-weight = 1.0`
- `rag.retrieval.keyword-weight = 1.0`
- `rag.chunking.strategy = token`

说明：

- 当前开发配置已经启用混合检索。
- 当前代码已经支持语义分块，但当前开发配置默认仍使用 `token` 分块，而不是 `semantic`。

## 3. 文档上传与入库流程

上传入口是 `POST /rag/uploadRagDoc`，核心流程位于 `DocumentServiceImpl.loadText(...)`。

当前流程如下：

1. 读取上传文本
2. 写入原始 metadata：`fileName`、`userId`
3. 调用 `semanticDocumentChunker.splitDocuments(...)` 执行分块
4. 为每个 chunk 补齐 metadata：
   - `fileName`
   - `userId`
   - `source`
   - `documentId`
   - `chunkId`
   - `chunkSeq`
5. 调用 `redisVectorStore.add(...)` 写入向量库
6. 调用 `keywordSearchService.indexChunks(...)` 建立关键词索引
7. 把文档索引信息写入 `t_rag_document`

## 4. 检索流程

检索主流程位于 `DocumentServiceImpl.doSearchWithTrace(...)`。

当前执行顺序如下：

1. 规范化 `topK`
2. 读取配置中的 `vectorRecallK` 与 `keywordRecallK`
3. 执行向量召回：`vectorRecall(question, userId, vectorRecallK)`
4. 执行关键词召回：`keywordSearchService.search(question, userId, keywordRecallK)`
5. 执行融合：`ragFusionService.fuse(vectorHits, keywordHits, finalTopK)`
6. 返回融合后的最终文档列表

其中：

- 向量检索通过 `RedisVectorStore.similaritySearch(...)` 完成
- 向量检索使用 `filterExpression("userId == '...'")` 做用户隔离
- 关键词检索通过 RediSearch 完成，并同样按 `userId` 过滤
- 融合算法使用 RRF，默认向量权重和关键词权重相同

## 5. 语义分块与混合检索的当前状态

当前代码状态可以概括为：

- 语义分块：代码已实现，但当前开发配置默认未启用
- 混合检索：代码已实现，当前开发配置已启用

如果后续需要启用“语义分块 + 混合检索”的组合，应至少：

1. 将 `rag.chunking.strategy` 切换为 `semantic`
2. 重新上传文档，使新的 chunk 重新写入向量库和关键词索引
3. 再结合 benchmark 调整召回数与融合参数

## 6. 对外接口

当前与 RAG 主流程直接相关的接口包括：

- `POST /rag/uploadRagDoc`：上传文档并建立索引
- `GET /rag/doSearch`：执行手动检索
- `POST /rag/search`：检索后生成知识问答结果
- `GET /rag/docs`：查询当前用户已上传文档
- `GET /rag/config` / `POST /rag/config`：读取当前手动 RAG 配置快照
- `POST /rag/benchmark/evaluate`：评测检索命中率

## 7. 说明

本文档只保留当前实现流程的简要说明，详细细节、分块规则、融合逻辑与配置解释统一放在：

- `docs/语义分块与混合检索实现说明.md`
- `docs/API接口文档.md`

## 8. 重排补充说明

当前检索链路在 RRF 融合之后，已经支持可选的重排阶段。

实际顺序补充如下：

1. 向量召回
2. 关键词召回
3. RRF 融合得到候选集
4. 若启用 `rerank-enabled`，则对融合候选执行启发式重排
5. 返回最终 TopK 结果

当前重排阶段位于“融合之后、最终结果返回之前”，不会替代向量召回、关键词召回或 RRF 融合本身。

说明：

- 当 `rerank-enabled = false` 时，系统仍按融合结果直接截断返回。
- 当 `rerank-enabled = true` 时，系统会先保留更大的融合候选池，再执行重排并返回最终 TopK。
- 当前重排主要用于在融合结果基础上进一步优化最终排序顺序。
