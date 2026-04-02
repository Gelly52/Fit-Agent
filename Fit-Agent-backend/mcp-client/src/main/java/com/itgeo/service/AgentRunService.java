package com.itgeo.service;

import com.itgeo.bean.AgentRunDetailResponse;
import com.itgeo.bean.AgentRunListItemResponse;
import com.itgeo.bean.ChatEntity;
import com.itgeo.pojo.AgentRun;

import java.util.List;

/**
 * Agent run / step 持久化与查询服务契约。
 *
 * 职责：
 * 1. 管理 run / step 的创建与状态流转；
 * 2. 提供运行列表与运行详情查询能力。
 */
public interface AgentRunService {

/**
     * 按 userId + botMsgId 查询既有 run，供受理层做幂等判断。
     *
     * @param userId 用户ID
     * @param botMsgId 机器人消息ID
     * @return 已存在的 run；不存在时返回 null
     */
    AgentRun findByUserIdAndBotMsgId(Long userId, String botMsgId);

/**
     * 创建一条 run 主记录。
     *
     * @param userId 用户ID
     * @param chatSessionId 会话ID
     * @param botMsgId 机器人消息ID
     * @param requestText 请求文本
     * @return run 主记录ID
     */
    Long createRun(Long userId, Long chatSessionId, String botMsgId, String requestText);

/**
     * 为指定 run 初始化固定 step 记录。
     *
     * @param runId run 主记录ID
     * @param chatEntity 聊天实体对象
     */
    void initSteps(Long runId, ChatEntity chatEntity);

/**
     * 将 run 状态更新为 running。
     *
     * @param runId run 主记录ID
     */
    void markRunRunning(Long runId);

/**
     * 将 run 状态更新为 success，并记录结果快照。
     *
     * @param runId run 主记录ID
     * @param resultJson 结果JSON字符串
     */
    void markRunSuccess(Long runId, String resultJson);

/**
     * 将 run 状态更新为 failed，并记录错误信息。
     *
     * @param runId run 主记录ID
     * @param errorMessage 错误消息
     */
    void markRunFailed(Long runId, String errorMessage);

/**
     * 将指定 step 状态更新为 running。
     *
     * @param runId run 主记录ID
     * @param stepNo 步骤编号
     * @param inputJson 输入JSON字符串
     */
    void markStepRunning(Long runId, Integer stepNo, String inputJson);

/**
     * 将指定 step 状态更新为 success，并记录输出快照。
     *
     * @param runId run 主记录ID
     * @param stepNo 步骤编号
     * @param outputJson 输出JSON字符串
     */
    void markStepSuccess(Long runId, Integer stepNo, String outputJson);

/**
     * 将指定 step 状态更新为 failed，并记录错误信息。
     *
     * @param runId run 主记录ID
     * @param stepNo 步骤编号
     * @param errorMessage 错误消息
     */
    void markStepFailed(Long runId, Integer stepNo, String errorMessage);

/**
     * 查询当前用户最近的 run 列表。
     *
     * @param userId 用户ID
     * @param status 可选状态过滤
     * @param limit 返回条数
     * @return 运行列表
     */
    List<AgentRunListItemResponse> listRuns(Long userId, String status, Integer limit);

/**
     * 查询当前用户指定 run 的详情及 step 列表。
     *
     * @param userId 用户ID
     * @param runId 运行ID
     * @return 运行详情；不存在时返回 null
     */
    AgentRunDetailResponse getRunDetail(Long userId, Long runId);
}
