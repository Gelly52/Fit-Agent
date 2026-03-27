package com.itgeo.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
@TableName("t_agent_step")
public class AgentStep {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("agent_run_id")
    private Long agentRunId;
    @TableField("step_no")
    private Integer stepNo;
    @TableField("step_name")
    private String stepName;
    @TableField("step_status")
    private String stepStatus;
    @TableField("tool_name")
    private String toolName;
    @TableField("input_json")
    private String inputJson;
    @TableField("output_json")
    private String outputJson;
    @TableField("error_message")
    private String errorMessage;
    @TableField("started_at")
    private LocalDateTime startedAt;
    @TableField("finished_at")
    private LocalDateTime finishedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
