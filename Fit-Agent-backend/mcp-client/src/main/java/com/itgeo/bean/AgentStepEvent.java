package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Agent step 状态事件载荷。
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AgentStepEvent {
    /** 所属 run ID。 */
    private Long runId;
    /** 当前步骤编号。 */
    private Integer stepNo;
    /** 当前步骤名称。 */
    private String stepName;
    /** 当前步骤状态。 */
    private String stepStatus;
    /** 面向前端的步骤提示信息。 */
    private String message;
}
