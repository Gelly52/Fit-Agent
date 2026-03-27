package com.itgeo.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itgeo.bean.ChatEntity;
import com.itgeo.mapper.AgentRunMapper;
import com.itgeo.mapper.AgentStepMapper;
import com.itgeo.pojo.AgentRun;
import com.itgeo.pojo.AgentStep;
import com.itgeo.service.AgentRunService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 运行记录与步骤记录持久化服务实现。
 *
 * 职责：
 * 1. 创建 t_agent_run 主记录；
 * 2. 初始化 t_agent_step 固定步骤；
 * 3. 在执行过程中维护 run / step 的状态、输入输出和错误信息。
 *
 * 说明：
 * - 当前实现只负责数据库状态维护，不负责真正的模型调用；
 * - stepStatus 使用数据库约定值：pending / running / success / failed；
 * - JSON 列当前统一按 String 存储，降低首期实现复杂度。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AgentRunServiceImpl implements AgentRunService {

    private static final List<String> DEFAULT_STEP_NAMES = List.of(
            "解析任务意图",
            "加载上下文",
            "执行核心能力",
            "生成结果",
            "完成回传"
    );

    @Resource
    private AgentRunMapper agentRunMapper;

    @Resource
    private AgentStepMapper agentStepMapper;

    @Override
    public AgentRun findByUserIdAndBotMsgId(Long userId, String botMsgId) {
        // 1. 幂等查询入参不完整时，直接返回 null
        if (userId == null || StrUtil.isBlank(botMsgId)) {
            return null;
        }

        // 2. 基于 userId + botMsgId 查询已有 run，供后续 execute 做幂等判断
        return agentRunMapper.selectOne(
                new LambdaQueryWrapper<AgentRun>()
                        .eq(AgentRun::getUserId, userId)
                        .eq(AgentRun::getBotMsgId, botMsgId.trim())
                        .last("limit 1"));
    }

    @Override
    public Long createRun(Long userId, Long chatSessionId, String botMsgId, String requestText) {
        // 1. 校验创建 run 所需核心参数
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(botMsgId)) {
            throw new IllegalArgumentException("botMsgId不能为空");
        }
        if (StrUtil.isBlank(requestText)) {
            throw new IllegalArgumentException("请求文本不能为空");
        }

        // 2. 初始化 run 主记录：
        //    - 初始状态固定 pending
        //    - startedAt / finishedAt 先不填
        AgentRun run = new AgentRun();
        run.setUserId(userId);
        run.setChatSessionId(chatSessionId);
        run.setBotMsgId(botMsgId.trim());
        run.setRequestText(requestText.trim());
        run.setStatus("pending");
        agentRunMapper.insert(run);

        // 3. 返回数据库生成的 runId，供后续步骤初始化与状态流转使用
        return run.getId();
    }

    @Override
    public void initSteps(Long runId, ChatEntity chatEntity) {
        // 1. run 主记录必须先存在，步骤记录才能挂到对应的 run 下
        ensureRunExists(runId);

        // 2. 当前固定初始化 5 个步骤；chatEntity 参数先保留，后续可用于按意图动态定制步骤
        for (int i = 0; i < DEFAULT_STEP_NAMES.size(); i++) {
            AgentStep step = new AgentStep();
            step.setAgentRunId(runId);
            step.setStepNo(i + 1);
            step.setStepName(DEFAULT_STEP_NAMES.get(i));
            step.setStepStatus("pending");
            agentStepMapper.insert(step);
        }
    }

    @Override
    public void markRunRunning(Long runId) {
        // 1. 确保目标 run 存在
        ensureRunExists(runId);

        // 2. 更新 run 状态为 running，并记录开始时间
        AgentRun update = new AgentRun();
        update.setId(runId);
        update.setStatus("running");
        update.setStartedAt(LocalDateTime.now());

        agentRunMapper.updateById(update);
    }

    @Override
    public void markRunSuccess(Long runId, String resultJson) {
        // 1. 确保目标 run 存在
        ensureRunExists(runId);

        // 2. 更新 run 状态为 success，记录结果摘要和完成时间
        AgentRun update = new AgentRun();
        update.setId(runId);
        update.setStatus("success");
        update.setResultJson(normalizeJson(resultJson));
        update.setErrorMessage(null);
        update.setFinishedAt(LocalDateTime.now());

        agentRunMapper.updateById(update);
    }

    @Override
    public void markRunFailed(Long runId, String errorMessage) {
        // 1. 确保目标 run 存在
        ensureRunExists(runId);

        // 2. 更新 run 状态为 failed，记录错误信息和完成时间
        AgentRun update = new AgentRun();
        update.setId(runId);
        update.setStatus("failed");
        update.setErrorMessage(normalizeErrorMessage(errorMessage, "Agent执行失败"));
        update.setFinishedAt(LocalDateTime.now());

        agentRunMapper.updateById(update);
    }

    @Override
    public void markStepRunning(Long runId, Integer stepNo, String inputJson) {
        // 1. 先查出目标步骤，确保该 run 下这个 stepNo 真实存在
        AgentStep existing = findRequiredStep(runId, stepNo);

        // 2. 更新步骤状态为 running，并记录输入和开始时间
        AgentStep update = new AgentStep();
        update.setId(existing.getId());
        update.setStepStatus("running");
        update.setInputJson(normalizeJson(inputJson));
        update.setStartedAt(LocalDateTime.now());

        agentStepMapper.updateById(update);
    }

    @Override
    public void markStepSuccess(Long runId, Integer stepNo, String outputJson) {
        // 1. 查出目标步骤
        AgentStep existing = findRequiredStep(runId, stepNo);

        // 2. 更新步骤状态为 success，记录输出和完成时间
        AgentStep update = new AgentStep();
        update.setId(existing.getId());
        update.setStepStatus("success");
        update.setOutputJson(normalizeJson(outputJson));
        update.setErrorMessage(null);
        update.setFinishedAt(LocalDateTime.now());

        agentStepMapper.updateById(update);
    }

    @Override
    public void markStepFailed(Long runId, Integer stepNo, String errorMessage) {
        // 1. 查出目标步骤
        AgentStep existing = findRequiredStep(runId, stepNo);

        // 2. 更新步骤状态为 failed，记录错误和完成时间
        AgentStep update = new AgentStep();
        update.setId(existing.getId());
        update.setStepStatus("failed");
        update.setErrorMessage(normalizeErrorMessage(errorMessage, "步骤执行失败"));
        update.setFinishedAt(LocalDateTime.now());

        agentStepMapper.updateById(update);
    }

    private void ensureRunExists(Long runId) {
        // 1. runId 不能为空
        if (runId == null) {
            throw new IllegalArgumentException("runId不能为空");
        }
        // 2. run 主记录必须存在，否则后续状态更新没有意义
        if (agentRunMapper.selectById(runId) == null) {
            throw new IllegalArgumentException("Agent运行记录不存在");
        }
    }

    private AgentStep findRequiredStep(Long runId, Integer stepNo) {
        // 1. runId 和 stepNo 都不能为空
        if (runId == null) {
            throw new IllegalArgumentException("runId不能为空");
        }
        if (stepNo == null) {
            throw new IllegalArgumentException("stepNo不能为空");
        }

        // 2. 按 runId + stepNo 查唯一的步骤记录
        AgentStep step = agentStepMapper.selectOne(
                new LambdaQueryWrapper<AgentStep>()
                        .eq(AgentStep::getAgentRunId, runId)
                        .eq(AgentStep::getStepNo, stepNo)
                        .last("limit 1")
        );

        // 3. 如果没查到，说明步骤未初始化或步骤编号非法
        if (step == null) {
            throw new IllegalArgumentException("Agent步骤不存在");
        }
        return step;
    }

    /**
     * 规范化 JSON 文本字段。
     *
     * 规则：
     * - 空白字符串统一转为 null；
     * - 非空时去掉首尾空格。
     *
     * 说明：
     * - 当前 JSON 列先按 String 持久化；
     * - 调用方应保证传入的是合法 JSON 字符串。
     *
     * @param json 原始 JSON 文本
     * @return 规范化后的 JSON 文本
     */
    private String normalizeJson(String json) {
        return StrUtil.isBlank(json) ? null : json.trim();
    }

    /**
     * 规范化错误消息，确保不超过 500 个字符。
     *
     * @param message 原始错误消息
     * @param defaultMessage 默认错误消息
     * @return 规范化后的错误消息
     */
    private String normalizeErrorMessage(String message, String defaultMessage) {
        String normalized = StrUtil.isBlank(message) ? defaultMessage : message.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
