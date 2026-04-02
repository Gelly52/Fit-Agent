# RAG MVP Fitness v2 Benchmark 结果整理（bge-m3）

## 1. 测试背景

- 整理时间：2026-04-02
- 调用方式：浏览器 Console 直接调用 `POST /rag/benchmark/evaluate`
- 数据集目录：`datasets/rag-mvp-fitness-v2`
- embedding provider：`bge-m3-http`
- embedding model：`bge-m3`
- 向量索引：`lee-vectorstore-bgem3`
- 用户隔离：`true`
- 隔离策略：`vectorstore_filterExpression_userId`
- 说明：本轮是在切换到本地 `bge-m3` 后，对同一套 `rag-mvp-fitness-v2` benchmark 数据重新评测；当前账号下 10 个知识文件均已上传且状态为 `READY`。从本轮 `retrievedFileNames` 与指标结果看，`bge-m3` 已明显优于旧默认 embedding 模型。

## 2. 汇总结果

| datasetName                        | runId | topK | questionCount | hitCount | hitRate | top1HitCount | top1HitRate | mrr | avgFirstHitFileRank | avgUniqueRetrievedFileCount |
| ---------------------------------- | ----: | ---: | ------------: | -------: | ------: | -----------: | ----------: | --: | ------------------: | --------------------------: |
| `fitness_mvp_v2_smoke_topk4_bgem3` |    20 |    4 |            10 |       10 |    1.00 |           10 |        1.00 |   1 |                   1 |                         2.6 |
| `fitness_mvp_v2_full_topk4_bgem3`  |    21 |    4 |            20 |       20 |    1.00 |           20 |        1.00 |   1 |                   1 |                         2.6 |
| `fitness_mvp_v2_full_topk6_bgem3`  |    22 |    6 |            20 |       20 |    1.00 |           20 |        1.00 |   1 |                   1 |                        4.05 |

## 3. 总体结论

1. 当前接口链路、用户隔离和 benchmark 指标计算均正常。
2. 本轮 3 组测试全部达到 `hitRate=1.00`、`top1HitRate=1.00`、`mrr=1.00`，说明当前 20 题全部命中且全部排在第一。
3. `topK` 从 `4` 提升到 `6` 后，命中与排序指标没有继续提升，说明在当前数据集上 `topK=4` 已足够。
4. `avgFirstHitFileRank` 稳定为 `1`，说明本轮不是“补召回”，而是目标文件已经稳定排在第一。
5. 与旧默认模型相比，本地 `bge-m3` 已同时解决“漏召回”和“非 Top1 排序偏差”两个问题。

## 4. Smoke TopK=4 结果说明

- 数据集：`fitness_mvp_v2_smoke_topk4_bgem3`
- runId：`20`
- 题量：`10`
- 命中率：`1.00`
- Top1 命中率：`1.00`

`smoke` 题集是 `full` 题集的子集，对应索引为：`1, 3, 5, 7, 9, 11, 13, 15, 17, 19`。

本轮 `smoke` 10 题全部 `hit=true` 且 `top1Hit=true`，无问题题目。

对照旧模型，`smoke` 中原先的主要问题题已全部修复：

| index | question                                     | expectedFileName                         | 旧结果        | 本轮结果  |
| ----: | -------------------------------------------- | ---------------------------------------- | ------------- | --------- |
|     7 | 训练前多久吃东西通常比较合适？               | `04_workout_fueling_and_carb_timing.txt` | 未命中        | Top1 命中 |
|    13 | 训练前热身一般建议做多久？                   | `07_warmup_and_cooldown_basics.txt`      | 命中但非 Top1 | Top1 命中 |
|    17 | 多数成年人为了恢复和健康，通常至少应睡多久？ | `09_sleep_and_recovery_basics.txt`       | 命中但非 Top1 | Top1 命中 |

## 5. Full TopK=4 结果说明

- 数据集：`fitness_mvp_v2_full_topk4_bgem3`
- runId：`21`
- 命中数：`20/20`
- 失败/非 Top1 题数：`0`

本轮 `full_topk4` 的 20 题全部 `hit=true` 且 `top1Hit=true`，无需再单独列出失败题目。

对照旧模型 `full_topk4`，以下历史问题已全部修复：

| index | question                                                   | expectedFileName                             | 旧结果                              | 本轮结果  |
| ----: | ---------------------------------------------------------- | -------------------------------------------- | ----------------------------------- | --------- |
|     4 | 运动中头晕、虚弱、恶心和明显异常疲劳，是否提示热相关风险？ | `02_hot_weather_hydration_safety.txt`        | 命中但非 Top1，`firstHitFileRank=2` | Top1 命中 |
|     7 | 训练前多久吃东西通常比较合适？                             | `04_workout_fueling_and_carb_timing.txt`     | 未命中                              | Top1 命中 |
|    13 | 训练前热身一般建议做多久？                                 | `07_warmup_and_cooldown_basics.txt`          | 命中但非 Top1，`firstHitFileRank=3` | Top1 命中 |
|    14 | 热身时更适合做动态动作还是长时间静态拉伸？                 | `07_warmup_and_cooldown_basics.txt`          | 命中但非 Top1，`firstHitFileRank=3` | Top1 命中 |
|    16 | 为什么不建议用特别极端的方式快速减重？                     | `08_fat_loss_and_calorie_deficit_basics.txt` | 命中但非 Top1，`firstHitFileRank=2` | Top1 命中 |
|    17 | 多数成年人为了恢复和健康，通常至少应睡多久？               | `09_sleep_and_recovery_basics.txt`           | 命中但非 Top1，`firstHitFileRank=2` | Top1 命中 |
|    20 | 使用肌酸时，一定要先做加载吗？                             | `10_creatine_monohydrate_basics.txt`         | 未命中                              | Top1 命中 |

## 6. Full TopK=6 结果说明

- 数据集：`fitness_mvp_v2_full_topk6_bgem3`
- runId：`22`
- 命中数：`20/20`
- 失败/非 Top1 题数：`0`

本轮 `full_topk6` 的 20 题全部 `hit=true` 且 `top1Hit=true`，无失败/非 Top1 题。

对照旧模型 `full_topk6`，以下历史问题已全部修复：

| index | question                                                   | expectedFileName                             | 旧结果                              | 本轮结果  |
| ----: | ---------------------------------------------------------- | -------------------------------------------- | ----------------------------------- | --------- |
|     4 | 运动中头晕、虚弱、恶心和明显异常疲劳，是否提示热相关风险？ | `02_hot_weather_hydration_safety.txt`        | 命中但非 Top1，`firstHitFileRank=2` | Top1 命中 |
|     7 | 训练前多久吃东西通常比较合适？                             | `04_workout_fueling_and_carb_timing.txt`     | 未命中                              | Top1 命中 |
|    13 | 训练前热身一般建议做多久？                                 | `07_warmup_and_cooldown_basics.txt`          | 命中但非 Top1，`firstHitFileRank=3` | Top1 命中 |
|    14 | 热身时更适合做动态动作还是长时间静态拉伸？                 | `07_warmup_and_cooldown_basics.txt`          | 命中但非 Top1，`firstHitFileRank=3` | Top1 命中 |
|    16 | 为什么不建议用特别极端的方式快速减重？                     | `08_fat_loss_and_calorie_deficit_basics.txt` | 命中但非 Top1，`firstHitFileRank=2` | Top1 命中 |
|    17 | 多数成年人为了恢复和健康，通常至少应睡多久？               | `09_sleep_and_recovery_basics.txt`           | 命中但非 Top1，`firstHitFileRank=2` | Top1 命中 |
|    20 | 使用肌酸时，一定要先做加载吗？                             | `10_creatine_monohydrate_basics.txt`         | 命中但非 Top1，`firstHitFileRank=5` | Top1 命中 |

## 7. TopK=4 与 TopK=6 对比

| 指标                        | full_topk4_bgem3 | full_topk6_bgem3 | 变化     |
| --------------------------- | ---------------: | ---------------: | -------- |
| hitCount                    |               20 |               20 | 不变     |
| hitRate                     |             1.00 |             1.00 | 不变     |
| top1HitCount                |               20 |               20 | 不变     |
| top1HitRate                 |             1.00 |             1.00 | 不变     |
| mrr                         |                1 |                1 | 不变     |
| avgFirstHitFileRank         |                1 |                1 | 不变     |
| avgUniqueRetrievedFileCount |              2.6 |             4.05 | 明显增大 |

重点解释：

- 在 `bge-m3` 下，`topK=4` 已经足以让全部题目 Top1 命中。
- `topK=6` 相比 `topK=4` 没有带来额外命中收益，只是扩大了返回候选文件的覆盖面。
- 从当前结果看，本轮数据集的主要改进来自 embedding 质量提升，而不是单纯增加 `topK`。

## 8. 与旧默认模型对比

| 数据集                       | 旧 runId | 新 runId | oldHitRate | newHitRate | oldTop1HitRate | newTop1HitRate |             oldMrr | newMrr | oldAvgFirstHitFileRank | newAvgFirstHitFileRank |
| ---------------------------- | -------: | -------: | ---------: | ---------: | -------------: | -------------: | -----------------: | -----: | ---------------------: | ---------------------: |
| `fitness_mvp_v2_smoke_topk4` |       17 |       20 |       0.90 |       1.00 |           0.70 |           1.00 | 0.7833333333333333 |      1 |     1.3333333333333333 |                      1 |
| `fitness_mvp_v2_full_topk4`  |       18 |       21 |       0.90 |       1.00 |           0.65 |           1.00 | 0.7583333333333334 |      1 |     1.3888888888888888 |                      1 |
| `fitness_mvp_v2_full_topk6`  |       19 |       22 |       0.95 |       1.00 |           0.65 |           1.00 | 0.7683333333333333 |      1 |     1.5789473684210527 |                      1 |

结论性对比：

1. `bge-m3` 把旧模型下的所有漏召回题全部修复，`hitRate` 全面提升到 `1.00`。
2. `bge-m3` 也把旧模型下所有“命中但排不到第一”的题全部拉升到 Top1，`top1HitRate` 全面提升到 `1.00`。
3. `mrr` 与 `avgFirstHitFileRank` 同时达到理论最优值，说明当前 20 题的目标文件都稳定排在第 1。
4. 从本轮结果看，`bge-m3` 在当前 `rag-mvp-fitness-v2` 数据集上，已经明显优于默认 `spring-ai-starter-model-transformerembedding` 模型。

## 9. 当前建议

- 如果当前目标是服务这 10 个 fitness 文档与这 20 道 benchmark 题，`bge-m3` 已可作为更优的 embedding 基线。
- 当前默认 `topK` 可继续保留在 `4`；在本数据集上继续增大到 `6` 的收益已经不明显。
- 如果要继续做更严格验证，后续可以优先从以下方向入手：
  1. 扩充 benchmark 题量，加入更口语化、更多近义改写的问题；
  2. 增加跨主题干扰题，验证排序稳定性；
  3. 在更多知识文件、更长文本和多用户并行场景下重复评测；
  4. 持续对旧模型下曾经出问题的 `index 4 / 7 / 13 / 14 / 16 / 17 / 20` 做专项回归测试。
