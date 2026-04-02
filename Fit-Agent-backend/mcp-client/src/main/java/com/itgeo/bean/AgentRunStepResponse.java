package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Agent run 详情中的步骤响应。
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentRunStepResponse {

    /** 步骤编号。 */
    private Integer stepNo;
    /** 步骤名称。 */
    private String stepName;
    /** 步骤状态。 */
    private String stepStatus;
    /** 关联工具名称。 */
    private String toolName;
    /** 步骤输入快照。 */
    private String inputJson;
    /** 步骤输出快照。 */
    private String outputJson;
    /** 步骤错误信息。 */
    private String errorMessage;
    /** 步骤开始时间。 */
    private LocalDateTime startedAt;
    /** 步骤完成时间。 */
    private LocalDateTime finishedAt;

}
