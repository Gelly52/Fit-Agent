package com.itgeo.pojo;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ToString
@TableName("t_body_metrics")
public class BodyMetrics {
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("record_date")
    private LocalDate recordDate;
    private BigDecimal weight;
    @TableField("body_fat")
    private BigDecimal bodyFat;
    @TableField("sleep_hours")
    private BigDecimal sleepHours;
    @TableField("fatigue_level")
    private String fatigueLevel;
    private String note;
    private String summary;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
