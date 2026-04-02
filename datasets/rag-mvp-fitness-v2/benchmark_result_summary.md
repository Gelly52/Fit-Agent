# RAG MVP Fitness v2 Benchmark 结果整理

## 1. 测试背景

- 整理时间：2026-04-01
- 调用方式：浏览器 Console 直接调用 `POST /rag/benchmark/evaluate`
- 数据集目录：`datasets/rag-mvp-fitness-v2`
- 用户隔离：`true`
- 隔离策略：`vectorstore_filterExpression_userId`
- 说明：从本轮贴出的 `retrievedFileNames` 看，结果中未再出现上一轮常见的 `v1` 文件名，当前结果更接近纯 `v2` 环境下的评测结果。

## 2. 汇总结果

| datasetName | runId | topK | questionCount | hitCount | hitRate | top1HitCount | top1HitRate | mrr | avgFirstHitFileRank | avgUniqueRetrievedFileCount |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `fitness_mvp_v2_smoke_topk4` | 17 | 4 | 10 | 9 | 0.90 | 7 | 0.70 | 0.7833333333333333 | 1.3333333333333333 | 3.0 |
| `fitness_mvp_v2_full_topk4` | 18 | 4 | 20 | 18 | 0.90 | 13 | 0.65 | 0.7583333333333334 | 1.3888888888888888 | 3.3 |
| `fitness_mvp_v2_full_topk6` | 19 | 6 | 20 | 19 | 0.95 | 13 | 0.65 | 0.7683333333333333 | 1.5789473684210527 | 5.0 |

## 3. 总体结论

1. 当前接口链路、用户隔离和 benchmark 指标计算均正常。
2. `topK` 从 `4` 提升到 `6` 后，`hitRate` 从 `0.90` 提升到 `0.95`，说明更深的召回能补回一部分漏召回题目。
3. `top1HitRate` 在 `topK=4` 和 `topK=6` 下都停留在 `0.65`，说明当前主要瓶颈已经从“是否召回到”转向“是否排到第一”。
4. `avgFirstHitFileRank` 在 `topK=6` 时反而高于 `topK=4`，说明新增命中的题目排位较靠后，属于“补召回”而非“强排序”。
5. 当前最优先问题仍是 `04_workout_fueling_and_carb_timing.txt` 的第 7 题；即使 `topK=6` 仍未命中。

## 4. Smoke TopK=4 结果说明

- 数据集：`fitness_mvp_v2_smoke_topk4`
- runId：`17`
- 题量：`10`
- 命中率：`0.90`
- Top1 命中率：`0.70`

`smoke` 题集是 `full` 题集的子集，对应索引为：`1, 3, 5, 7, 9, 11, 13, 15, 17, 19`。

按 `full` 结果对应索引回看，`smoke` 的主要问题题如下：

| index | question | expectedFileName | 结果 | 说明 |
| ---: | --- | --- | --- | --- |
| 7 | 训练前多久吃东西通常比较合适？ | `04_workout_fueling_and_carb_timing.txt` | 未命中 | 当前最优先问题；`topK=4/6` 都未命中 |
| 13 | 训练前热身一般建议做多久？ | `07_warmup_and_cooldown_basics.txt` | 命中但非 Top1 | 在 `full_topk4` 中 `firstHitFileRank=3` |
| 17 | 多数成年人为了恢复和健康，通常至少应睡多久？ | `09_sleep_and_recovery_basics.txt` | 命中但非 Top1 | 在 `full_topk4` 中 `firstHitFileRank=2` |

## 5. Full TopK=4 问题题目明细

- 数据集：`fitness_mvp_v2_full_topk4`
- runId：`18`
- 命中数：`18/20`
- 失败/非 Top1 题数：`7`

| index | question | expectedFileName | hit | top1Hit | firstHitFileRank | top1FileName |
| ---: | --- | --- | --- | --- | ---: | --- |
| 4 | 运动中头晕、虚弱、恶心和明显异常疲劳，是否提示热相关风险？ | `02_hot_weather_hydration_safety.txt` | true | false | 2 | `07_warmup_and_cooldown_basics.txt` |
| 7 | 训练前多久吃东西通常比较合适？ | `04_workout_fueling_and_carb_timing.txt` | false | false | - | `01_activity_targets_for_adults.txt` |
| 13 | 训练前热身一般建议做多久？ | `07_warmup_and_cooldown_basics.txt` | true | false | 3 | `01_activity_targets_for_adults.txt` |
| 14 | 热身时更适合做动态动作还是长时间静态拉伸？ | `07_warmup_and_cooldown_basics.txt` | true | false | 3 | `05_strength_training_recovery_planning.txt` |
| 16 | 为什么不建议用特别极端的方式快速减重？ | `08_fat_loss_and_calorie_deficit_basics.txt` | true | false | 2 | `09_sleep_and_recovery_basics.txt` |
| 17 | 多数成年人为了恢复和健康，通常至少应睡多久？ | `09_sleep_and_recovery_basics.txt` | true | false | 2 | `01_activity_targets_for_adults.txt` |
| 20 | 使用肌酸时，一定要先做加载吗？ | `10_creatine_monohydrate_basics.txt` | false | false | - | `06_overtraining_and_excess_fatigue_signs.txt` |

## 6. Full TopK=6 问题题目明细

- 数据集：`fitness_mvp_v2_full_topk6`
- runId：`19`
- 命中数：`19/20`
- 失败/非 Top1 题数：`7`

| index | question | expectedFileName | hit | top1Hit | firstHitFileRank | top1FileName |
| ---: | --- | --- | --- | --- | ---: | --- |
| 4 | 运动中头晕、虚弱、恶心和明显异常疲劳，是否提示热相关风险？ | `02_hot_weather_hydration_safety.txt` | true | false | 2 | `07_warmup_and_cooldown_basics.txt` |
| 7 | 训练前多久吃东西通常比较合适？ | `04_workout_fueling_and_carb_timing.txt` | false | false | - | `01_activity_targets_for_adults.txt` |
| 13 | 训练前热身一般建议做多久？ | `07_warmup_and_cooldown_basics.txt` | true | false | 3 | `01_activity_targets_for_adults.txt` |
| 14 | 热身时更适合做动态动作还是长时间静态拉伸？ | `07_warmup_and_cooldown_basics.txt` | true | false | 3 | `05_strength_training_recovery_planning.txt` |
| 16 | 为什么不建议用特别极端的方式快速减重？ | `08_fat_loss_and_calorie_deficit_basics.txt` | true | false | 2 | `09_sleep_and_recovery_basics.txt` |
| 17 | 多数成年人为了恢复和健康，通常至少应睡多久？ | `09_sleep_and_recovery_basics.txt` | true | false | 2 | `01_activity_targets_for_adults.txt` |
| 20 | 使用肌酸时，一定要先做加载吗？ | `10_creatine_monohydrate_basics.txt` | true | false | 5 | `06_overtraining_and_excess_fatigue_signs.txt` |

## 7. TopK=4 与 TopK=6 对比

| 指标 | full_topk4 | full_topk6 | 变化 |
| --- | ---: | ---: | --- |
| hitCount | 18 | 19 | `+1` |
| hitRate | 0.90 | 0.95 | 提升 |
| top1HitCount | 13 | 13 | 不变 |
| top1HitRate | 0.65 | 0.65 | 不变 |
| mrr | 0.7583333333333334 | 0.7683333333333333 | 小幅提升 |
| avgFirstHitFileRank | 1.3888888888888888 | 1.5789473684210527 | 变差 |
| avgUniqueRetrievedFileCount | 3.3 | 5.0 | 明显增大 |

重点解释：

- `topK=6` 相比 `topK=4`，主要是把第 `20` 题从“未命中”救成了“命中但排第 5”。
- 第 `7` 题在 `topK=6` 下仍未命中，说明这不是单纯扩大召回条数就能解决的问题。
- 因为新增命中的题目排位较靠后，所以 `avgFirstHitFileRank` 并没有变好。

## 8. 优先优化清单

1. `04_workout_fueling_and_carb_timing.txt`
   - 优先修复第 `7` 题：`训练前多久吃东西通常比较合适？`
   - 这是当前唯一在 `topK=6` 下仍未命中的题。
2. `07_warmup_and_cooldown_basics.txt`
   - 两道题都能召回，但都排不到第一。
   - 说明主题相关性够，但排序信号不够强。
3. `10_creatine_monohydrate_basics.txt`
   - 第 `20` 题在 `topK=6` 下才勉强命中，且排到第 `5`。
   - “是否一定要加载”这类问法与当前知识文本的匹配还不够直接。
4. `08_fat_loss_and_calorie_deficit_basics.txt`、`09_sleep_and_recovery_basics.txt`
   - 当前属于“可命中但容易被其他健康/恢复类文档抢到 Top1”。
5. `02_hot_weather_hydration_safety.txt`
   - 其中一题能稳定 Top1，另一题仍存在明显排序偏差。

## 9. 当前建议

- 如果目标是继续拉高 `hitRate`，可以继续观察 `topK=8`，但预期收益有限。
- 如果目标是提升真实可用性，应优先优化排序质量，而不是继续单纯增大 `topK`。
- 后续优化可优先从以下方向入手：
  1. 强化知识文本中的关键词覆盖与问法贴合度；
  2. 为易混主题补充更明确的小节标题与高辨识度句式；
  3. 视情况引入 rerank 或混合检索策略；
  4. 对 `index 7 / 13 / 14 / 20` 这几题做专项回归测试。
