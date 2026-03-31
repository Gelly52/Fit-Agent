package com.itgeo.service;

import com.itgeo.bean.AgentRunDetailResponse;
import com.itgeo.bean.AgentRunListItemResponse;
import com.itgeo.bean.ChatEntity;
import com.itgeo.pojo.AgentRun;

import java.util.List;

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

    /**
     * 查询当前用户最近的 Agent 运行列表。
     *
     * @param userId 用户ID
     * @param status 可选状态过滤
     * @param limit 返回条数
     * @return 运行列表
     */
    List<AgentRunListItemResponse> listRuns(Long userId, String status, Integer limit);

    /**
     * 查询当前用户指定运行的详情与步骤。
     *
     * @param userId 用户ID
     * @param runId 运行ID
     * @return 运行详情；不存在时返回 null
     */
    AgentRunDetailResponse getRunDetail(Long userId, Long runId);
}
