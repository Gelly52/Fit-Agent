package com.itgeo.service;

import com.itgeo.bean.ChatEntity;
import com.itgeo.pojo.AgentRun;

public interface AgentRunService {

    /**
     * 根据用户ID和机器人消息ID查询智能体运行记录
     * @param userId 用户ID
     * @param botMsgId 机器人消息ID
     * @return 智能运行记录对象
     */
    AgentRun findByUserIdAndBotMsgId(Long userId, String botMsgId);

    /**
     * 创建智能体运行记录
     * @param userId 用户ID
     * @param chatSessionId 会话ID
     * @param botMsgId 机器人消息ID
     * @param requestText 请求文本
     * @return 智能体运行记录ID
     */
    Long createRun(Long userId, Long chatSessionId, String botMsgId, String requestText);

    /**
     * 初始化智能体运行记录的步骤
     * @param runId 智能体运行记录ID
     * @param chatEntity 聊天实体对象
     */
    void initSteps(Long runId, ChatEntity chatEntity);

    /**
     * 标记智能体运行记录为运行中
     * @param runId 智能体运行记录ID
     */
    void markRunRunning(Long runId);

    /**
     * 标记智能体运行记录为成功
     * @param runId 智能体运行记录ID
     * @param resultJson 结果JSON字符串
     */
    void markRunSuccess(Long runId, String resultJson);

    /**
     * 标记智能体运行记录为失败
     * @param runId 智能体运行记录ID
     * @param errorMessage 错误消息
     */
    void markRunFailed(Long runId, String errorMessage);

    /**
     * 标记智能体运行记录的步骤为运行中
     * @param runId 智能体运行记录ID
     * @param stepNo 步骤编号
     * @param inputJson 输入JSON字符串
     */
    void markStepRunning(Long runId, Integer stepNo, String inputJson);

    /**
     * 标记智能体运行记录的步骤为成功
     * @param runId 智能体运行记录ID
     * @param stepNo 步骤编号
     * @param outputJson 输出JSON字符串
     */
    void markStepSuccess(Long runId, Integer stepNo, String outputJson);

    /**
     * 标记智能体运行记录的步骤为失败
     * @param runId 智能体运行记录ID
     * @param stepNo 步骤编号
     * @param errorMessage 错误消息
     */
    void markStepFailed(Long runId, Integer stepNo, String errorMessage);
}
