# Fit-Agent 数据库建表说明

## 1. SQL 文件

- 建表脚本：`Fit-Agent-backend/mcp-server/src/main/resources/sql/fit_agent_init.sql`
- 数据库名：`springai-items-mcp`
- 脚本先执行以下语句：

```sql
CREATE DATABASE IF NOT EXISTS `springai-items-mcp`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `springai-items-mcp`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
```

- 所有表都通过 `CREATE TABLE IF NOT EXISTS` 创建
- 脚本最后执行：

```sql
SET FOREIGN_KEY_CHECKS = 1;
```

## 2. 脚本统一规则

- 存储引擎统一为 `InnoDB`
- 字符集统一为 `utf8mb4`
- 排序规则统一为 `utf8mb4_unicode_ci`
- 主键统一使用 `BIGINT` 自增
- 大部分表带 `created_at`，需要更新记录的表带 `updated_at`
- 外键直接写在建表语句里，没有拆到单独的 `ALTER TABLE`
- 脚本按 Phase 分段，但实际是在同一个初始化文件里一次性建表

## 3. 建表总览

| Phase   | 表名                                                      |
| ------- | --------------------------------------------------------- |
| Phase 0 | `t_user`、`t_user_login_session`                          |
| Phase 1 | `t_training_log`、`t_training_exercise`、`t_body_metrics` |
| Phase 2 | `t_chat_session`、`t_chat_message`                        |
| Phase 3 | `t_rag_document`、`t_rag_config`、`t_rag_benchmark_run`   |
| Phase 4 | `t_dashboard_summary`、`t_agent_run`、`t_agent_step`      |

总计 13 张表。

## 4. Phase 0：用户与登录会话

### `t_user`

- 作用：用户主数据表
- 字段：`id`、`user_key`、`username`、`password_hash`、`nickname`、`email`、`phone`、`status`、`last_login_at`、`created_at`、`updated_at`
- 主键：`id`
- 唯一键：`uk_user_user_key`、`uk_user_username`、`uk_user_email`
- 普通索引：`idx_user_status`
- 备注：`status` 默认 `1`，`updated_at` 使用 `ON UPDATE CURRENT_TIMESTAMP`

### `t_user_login_session`

- 作用：登录会话表
- 字段：`id`、`user_id`、`refresh_token_hash`、`client_ip`、`user_agent`、`expired_at`、`revoked_at`、`created_at`
- 主键：`id`
- 唯一键：`uk_login_session_token`
- 普通索引：`idx_login_session_user`、`idx_login_session_expired`
- 外键：`fk_login_session_user`，`user_id -> t_user.id`

## 5. Phase 1：训练与身体指标

### `t_training_log`

- 作用：训练主表
- 字段：`id`、`user_id`、`training_date`、`summary`、`primary_muscle_group`、`total_volume`、`source`、`created_at`、`updated_at`
- 主键：`id`
- 唯一键：`uk_training_user_date`，约束 `user_id + training_date`
- 普通索引：`idx_training_user_date`、`idx_training_created_at`
- 外键：`fk_training_log_user`，`user_id -> t_user.id`
- 备注：`total_volume` 默认 `0`，`source` 默认 `manual`

### `t_training_exercise`

- 作用：训练动作明细表
- 字段：`id`、`training_log_id`、`exercise_name`、`sets`、`reps`、`weight`、`order_num`、`estimated_muscle_group`、`created_at`
- 主键：`id`
- 普通索引：`idx_exercise_training_log_id`
- 外键：`fk_exercise_training_log`，`training_log_id -> t_training_log.id`
- 备注：`sets`、`reps` 默认 `1`，`weight` 默认 `0`，`order_num` 默认 `0`

### `t_body_metrics`

- 作用：身体指标事实表
- 字段：`id`、`user_id`、`record_date`、`weight`、`body_fat`、`sleep_hours`、`fatigue_level`、`note`、`summary`、`created_at`、`updated_at`
- 主键：`id`
- 唯一键：`uk_metrics_user_date`，约束 `user_id + record_date`
- 普通索引：`idx_metrics_user_date`、`idx_metrics_created_at`
- 外键：`fk_body_metrics_user`，`user_id -> t_user.id`

## 6. Phase 2：聊天会话与消息

### `t_chat_session`

- 作用：聊天会话表
- 字段：`id`、`session_code`、`user_id`、`scene_type`、`title`、`last_bot_msg_id`、`created_at`、`updated_at`
- 主键：`id`
- 唯一键：`uk_chat_session_code`
- 普通索引：`idx_chat_session_user`、`idx_chat_session_scene`
- 外键：`fk_chat_session_user`，`user_id -> t_user.id`
- 备注：`scene_type` 默认 `chat`

### `t_chat_message`

- 作用：聊天消息表
- 字段：`id`、`session_id`、`seq_no`、`role`、`message_type`、`source_type`、`content`、`bot_msg_id`、`sources_json`、`created_at`、`updated_at`
- 主键：`id`
- 普通索引：`idx_chat_message_session_seq`、`idx_chat_message_bot`、`idx_chat_message_created`
- 外键：`fk_chat_message_session`，`session_id -> t_chat_session.id`
- 备注：`message_type` 默认 `text`，`source_type` 默认 `chat`，`sources_json` 使用 `JSON`

## 7. Phase 3：RAG 文档、配置与评测

### `t_rag_document`

- 作用：RAG 文档元数据表
- 字段：`id`、`user_id`、`file_name`、`source_count`、`file_hash`、`file_type`、`file_size`、`storage_uri`、`chunk_count`、`vector_status`、`metadata_json`、`created_at`、`updated_at`
- 主键：`id`
- 唯一键：`uk_rag_file_hash`
- 普通索引：`idx_rag_document_user`、`idx_rag_document_status`
- 外键：`fk_rag_document_user`，`user_id -> t_user.id`
- 备注：`user_id` 可为空；`vector_status` 默认 `pending`；`metadata_json` 使用 `JSON`

### `t_rag_config`

- 作用：RAG 配置表
- 字段：`id`、`scope_type`、`scope_id`、`config_key`、`config_value_json`、`description`、`created_by_user_id`、`created_at`、`updated_at`
- 主键：`id`
- 唯一键：`uk_rag_config_scope`，约束 `scope_type + scope_id + config_key`
- 普通索引：`idx_rag_config_creator`
- 外键：`fk_rag_config_creator`，`created_by_user_id -> t_user.id`
- 备注：`config_value_json` 使用 `JSON`

### `t_rag_benchmark_run`

- 作用：RAG 评测任务表
- 字段：`id`、`user_id`、`dataset_name`、`question_count`、`status`、`config_snapshot_json`、`result_json`、`created_at`、`updated_at`
- 主键：`id`
- 普通索引：`idx_benchmark_user`、`idx_benchmark_status`
- 外键：`fk_benchmark_user`，`user_id -> t_user.id`
- 备注：`status` 默认 `pending`；`config_snapshot_json` 和 `result_json` 使用 `JSON`

## 8. Phase 4：Dashboard 与 Agent

### `t_dashboard_summary`

- 作用：Dashboard 聚合快照表
- 字段：`id`、`user_id`、`summary_date`、`summary_type`、`training_days`、`total_volume`、`avg_weight`、`weight_change`、`fatigue_level`、`result_summary_json`、`report_content`、`generated_at`
- 主键：`id`
- 唯一键：`uk_dashboard_summary`，约束 `user_id + summary_date + summary_type`
- 普通索引：`idx_dashboard_generated`
- 外键：`fk_dashboard_summary_user`，`user_id -> t_user.id`
- 备注：`summary_type` 默认 `daily`；`result_summary_json` 使用 `JSON`

### `t_agent_run`

- 作用：Agent 运行主表
- 字段：`id`、`user_id`、`chat_session_id`、`request_text`、`status`、`result_json`、`error_message`、`started_at`、`finished_at`、`created_at`、`bot_msg_id`
- 主键：`id`
- 唯一键：`uk_agent_run_user_bot_msg`，约束 `user_id + bot_msg_id`
- 普通索引：`idx_agent_run_user`、`idx_agent_run_status`、`idx_agent_run_session`
- 外键：`fk_agent_run_user`、`fk_agent_run_session`
- 外键关系：`user_id -> t_user.id`，`chat_session_id -> t_chat_session.id`
- 备注：`status` 默认 `pending`，`result_json` 使用 `JSON`

### `t_agent_step`

- 作用：Agent 步骤表
- 字段：`id`、`agent_run_id`、`step_no`、`step_name`、`step_status`、`tool_name`、`input_json`、`output_json`、`error_message`、`started_at`、`finished_at`、`created_at`
- 主键：`id`
- 唯一键：`uk_agent_step_run_no`，约束 `agent_run_id + step_no`
- 普通索引：`idx_agent_step_run`
- 外键：`fk_agent_step_run`，`agent_run_id -> t_agent_run.id`
- 备注：`step_status` 默认 `pending`；`input_json`、`output_json` 使用 `JSON`

## 9. 外键关系

- `t_user` 被以下表引用：
  - `t_user_login_session`
  - `t_training_log`
  - `t_body_metrics`
  - `t_chat_session`
  - `t_rag_document`
  - `t_rag_config`
  - `t_rag_benchmark_run`
  - `t_dashboard_summary`
  - `t_agent_run`
- `t_training_log` 被 `t_training_exercise` 引用
- `t_chat_session` 被 `t_chat_message` 和 `t_agent_run` 引用
- `t_agent_run` 被 `t_agent_step` 引用


