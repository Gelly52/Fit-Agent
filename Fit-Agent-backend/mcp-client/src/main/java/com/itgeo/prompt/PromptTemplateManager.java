package com.itgeo.prompt;

import com.itgeo.pojo.BodyMetrics;
import com.itgeo.pojo.TrainingLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提示词模板管理器
 * 负责管理所有 Agent 提示词模板，支持动态上下文注入
 */
@Component
@Slf4j
public class PromptTemplateManager {

    // ==================== 核心系统提示词 ====================

    /**
     * Agent 主系统提示词（精简版）
     * 定义角色、能力、对话原则、安全边界
     */
    private static final String AGENT_SYSTEM_PROMPT = """
            你是 FitAgent，专业健身数据助手。

            ## 核心能力
            记录训练/体测数据、分析历史趋势、提供个性化建议、解答健身问题。

            ## 对话原则
            1. **数据驱动**: 涉及用户情况时，先调用查询工具获取数据，基于实际数据回答
            2. **主动记录**: 用户描述训练内容时（"今天练了..."），询问是否记录
            3. **渐进提问**: 复杂需求分步确认（目标→现状→方案）
            4. **专业友好**: 术语需简要解释，适当鼓励但不空泛

            ## 安全边界
            遇到急性疼痛、伤病、特殊人群（孕期/术后/慢性病）、药物补剂时，建议咨询医生/专业教练。
            禁止: 诊断伤病、推荐极端饮食、鼓励过度训练。

            ## 工具使用
            - 记录训练: createTrainingLog（确认日期、摘要、肌群）
            - 查看历史: queryTrainingLogs（userId + 时间范围，倒序）
            - 分析进展: 先调用 queryTrainingLogs 查询至少14天数据，再分析频率/肌群分布并给出建议
            - 记录体测: createBodyMetrics（至少一项指标）
            - 查看趋势: queryBodyMetrics（时间段 + 计算变化）

            ### 知识库增强
            - 开启时: 优先使用知识库内容回答专业问题
            - 未开启: 基于通用知识回答，但要说明当前知识库未查到相关信息
            
            ### 联网增强
            - 开启时: 用于查询最新健身资讯、赛事信息
            - 未开启: 基于训练数据回答，但要说明未检索到网络信息

            ## 用户上下文
            {userContext}

            ## 当前日期
            {currentDate}
            
            {thinkingInstruction}

            ## 用户问题
            {question}

            回答要求: 简洁直接、基于数据、结构化呈现（表格/列表）。
            你必须严格按以下格式输出，不能省略，不能变形，不能拼错标签：
            <thinking>
            这里写分析摘要(思考过程)
            </thinking>
            这里写正式回答
            """;

    /**
     * 思考过程输出指令
     * 所有模板都需要包含此指令，引导模型输出结构化的思考过程
     */
    private static final String THINKING_INSTRUCTION = """

             ## 思考过程输出要求
             在回答问题时，请按以下格式组织你的输出：
             1. 首先在 <thinking></thinking> 标签内写出你的思考过程，包括：
                - 分析用户的真实意图
                - 判断是否需要调用工具
                - 如果需要调用工具，说明调用哪个工具、为什么
                - 如果不需要工具，说明你的回答思路
             2. 然后给出最终回答（不要包含在任何标签内）

             示例格式：

             <thinking>
             用户询问最近的训练情况，我需要先调用 queryTrainingLogs 工具查询用户最近14天的训练数据，
             然后基于数据分析训练频率和肌群分布，给出客观的评价和建议。
             </thinking>
             根据您最近14天的训练数据，您一共训练了8次...
             """;


    /**
     * 普通 Chat 提示词（无用户上下文）
     */
    private static final String CHAT_PROMPT_TEMPLATE = """
            你是 FitAgent，专业健身助手。

            请回答用户的健身相关问题，保持专业、友好、简洁。
            如果涉及伤病、特殊人群、药物补剂，建议咨询专业人士。

            ## 用户上下文
            {userContext}
            
            ## 当前日期
            {currentDate}
            
            {thinkingInstruction}

            ## 用户问题
            {question}
            """;

    /**
     * RAG 增强提示词
     * 当开启知识库增强时使用
     */
    private static final String RAG_PROMPT_TEMPLATE = """
            你是 FitAgent，正在使用知识库增强模式回答问题。

            ## 知识库内容
            {context}

            ## 用户问题
            {question}
            
            {thinkingInstruction}

            ## 回答要求
            1. 优先使用知识库内容，标注"根据资料..."
            2. 知识库不足时，补充通用知识并区分说明
            3. 确实无相关内容时，说"知识库中暂无相关资料"，然后基于通用知识简要回答
            4. 直接回答，使用列表/表格结构化呈现
            """;

    /**
     * 联网增强提示词
     * 当开启联网搜索时使用
     */
    private static final String INTERNET_PROMPT_TEMPLATE = """
            你是 FitAgent，正在使用联网搜索增强模式回答问题。
            
            ## 搜索结果
            {context}
            
            ## 用户问题
            {question}
            
            {thinkingInstruction}
            
            ## 回答要求
            1. **综合信息**: 整合多个搜索结果，给出全面的回答
            2. **时效性**: 优先使用最新的信息（注意发布日期）
            3. **可信度**: 优先引用权威来源（官方、学术、专业机构）
            4. **标注来源**: 重要信息可以标注来源网站
            5. **批判性**: 如果搜索结果有争议或不一致，指出并说明
            
            ## 回答格式
            - 直接回答问题
            - 重要信息可以标注 [来源: xxx]
            - 如果搜索结果不相关，说明"未找到相关实时信息"，然后基于通用知识回答
            """;

    /**
     * 混合增强提示词
     * 同时开启知识库和联网时使用
     */
    private static final String HYBRID_PROMPT_TEMPLATE = """
            你是 FitAgent，正在使用知识库+联网双重增强模式回答问题。
            
            ## 知识库内容
            {ragContext}
            
            ## 联网搜索结果
            {internetContext}
            
            ## 用户问题
            {question}
            
            {thinkingInstruction}
            
            ## 回答策略
            1. **优先级**: 知识库（专业资料）> 联网搜索（实时信息）> 通用知识
            2. **互补使用**:
               - 知识库提供理论基础和专业知识
               - 联网搜索提供最新资讯和实时信息
            3. **交叉验证**: 如果两个来源有冲突，说明差异并给出你的判断
            4. **标注来源**: 区分哪些来自知识库，哪些来自网络搜索
            
            ## 回答格式
            - 综合两个来源，给出完整回答
            - 必要时分段说明（理论部分 + 最新动态）
            - 保持专业性和可读性
            """;

    // ==================== 动态上下文构建 ====================

    /**
     * 构建用户上下文信息
     *
     * @param userId             用户ID
     * @param recentTrainingLogs 最近的训练日志（建议查询最近7-14天）
     * @param latestBodyMetrics  最新的身体指标
     * @return 格式化的用户上下文字符串
     */
    public String buildUserContext(
            Long userId,
            List<TrainingLog> recentTrainingLogs,
            BodyMetrics latestBodyMetrics
    ) {
        if (recentTrainingLogs == null || recentTrainingLogs.isEmpty()) {
            return String.format("""
                    **用户ID**: %d
                    **训练状态**: 新用户，暂无训练记录
                    **建议**: 主动引导用户开始记录训练数据
                    """, userId);
        }

        // 分析训练频率
        int trainingCount = recentTrainingLogs.size();
        LocalDate oldestDate = recentTrainingLogs.stream()
                .map(TrainingLog::getTrainingDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        LocalDate latestDate = recentTrainingLogs.stream()
                .map(TrainingLog::getTrainingDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
        long daySpan = java.time.temporal.ChronoUnit.DAYS.between(oldestDate, latestDate) + 1;

        // 分析主要肌群分布
        Map<String, Long> muscleGroupCount = recentTrainingLogs.stream()
                .filter(log -> log.getPrimaryMuscleGroup() != null)
                .collect(Collectors.groupingBy(
                        TrainingLog::getPrimaryMuscleGroup,
                        Collectors.counting()
                ));

        String muscleDistribution = muscleGroupCount.isEmpty()
                ? "未记录肌群信息"
                : muscleGroupCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> e.getKey() + ": " + e.getValue() + "次")
                .collect(Collectors.joining(", "));

        // 评估训练活跃度
        String activityLevel;
        double avgPerWeek = trainingCount * 7.0 / daySpan;
        if (avgPerWeek >= 4) {
            activityLevel = "高度活跃（每周" + String.format("%.1f", avgPerWeek) + "次）";
        } else if (avgPerWeek >= 2) {
            activityLevel = "中等活跃（每周" + String.format("%.1f", avgPerWeek) + "次）";
        } else {
            activityLevel = "活跃度较低（每周" + String.format("%.1f", avgPerWeek) + "次），需要鼓励";
        }

        // 身体指标信息
        String metricsInfo = "未记录";
        if (latestBodyMetrics != null) {
            metricsInfo = String.format("体重 %s kg, 体脂率 %s%%, 记录于 %s",
                    latestBodyMetrics.getWeight() != null ? latestBodyMetrics.getWeight() : "未记录",
                    latestBodyMetrics.getBodyFat() != null ? latestBodyMetrics.getBodyFat() : "未记录",
                    latestBodyMetrics.getRecordDate()
            );
        }

        return String.format("""
                        **用户ID**: %d
                        **训练概况** (最近%d天):
                        - 训练次数: %d次
                        - 最近训练: %s
                        - 活跃度: %s
                        - 主要肌群: %s
                        
                        **身体指标**:
                        - %s
                        
                        **分析建议**:
                        - 回答问题时，优先基于上述数据
                        - 如果用户询问训练情况，先调用查询工具获取最新数据
                        - 如果发现训练不平衡（某肌群过多/过少），在合适时机提醒
                        """,
                userId,
                daySpan,
                trainingCount,
                latestDate,
                activityLevel,
                muscleDistribution,
                metricsInfo
        );
    }

    /**
     * 构建 Agent 主提示词（带用户上下文）
     */
    public String buildAgentPrompt(String userContext, String question) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        return AGENT_SYSTEM_PROMPT
                .replace("{userContext}", userContext != null ? userContext : "暂无用户上下文")
                .replace("{currentDate}", currentDate)
                .replace("{thinkingInstruction}", THINKING_INSTRUCTION)
                .replace("{question}", question);
    }

    /**
     * 构建普通 Chat 提示词（无用户上下文）
     */
    public String buildChatPrompt(String userContext, String question) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
        return CHAT_PROMPT_TEMPLATE
                .replace("{userContext}", userContext != null ? userContext : "暂无用户上下文")
                .replace("{currentDate}", currentDate)
                .replace("{thinkingInstruction}", THINKING_INSTRUCTION)
                .replace("{question}", question);
    }

    /**
     * 构建 RAG 增强提示词
     */
    public String buildRagPrompt(String context, String question) {
        return RAG_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{thinkingInstruction}", THINKING_INSTRUCTION)
                .replace("{question}", question);
    }

    /**
     * 构建联网增强提示词
     */
    public String buildInternetPrompt(String context, String question) {
        return INTERNET_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{thinkingInstruction}", THINKING_INSTRUCTION)
                .replace("{question}", question);
    }

    /**
     * 构建混合增强提示词
     */
    public String buildHybridPrompt(String ragContext, String internetContext, String question) {
        return HYBRID_PROMPT_TEMPLATE
                .replace("{ragContext}", ragContext)
                .replace("{internetContext}", internetContext)
                .replace("{thinkingInstruction}", THINKING_INSTRUCTION)
                .replace("{question}", question);
    }

    /**
     * 获取场景化提示词片段
     * 可以在主提示词基础上追加特定场景的指令
     */
    public String getScenarioPrompt(String scenario) {
        return switch (scenario) {
            case "RECORD_TRAINING" -> """
                    
                    ## 当前任务: 帮助用户记录训练
                    1. 确认训练日期（默认今天）
                    2. 确认训练内容和主要肌群
                    3. 询问是否需要记录动作明细
                    4. 记录成功后，简要鼓励并提示可以查看历史记录
                    """;
            case "ANALYZE_PROGRESS" -> """
                    
                    ## 当前任务: 分析用户训练进展
                    1. 先调用 queryTrainingLogs 查询至少14天数据
                    2. 分析训练频率、肌群分布、训练量趋势
                    3. 给出具体的数据支撑的建议
                    4. 避免空泛鼓励，必须基于实际数据
                    """;
            case "PLAN_TRAINING" -> """
                    
                    ## 当前任务: 制定训练计划
                    1. 了解用户目标（增肌/减脂/力量/塑形）
                    2. 查询历史训练情况
                    3. 询问可用训练时间和频率
                    4. 考虑肌群恢复周期，给出具体计划
                    5. 计划要包含: 训练频率、肌群安排、动作建议
                    """;
            default -> "";
        };
    }
}