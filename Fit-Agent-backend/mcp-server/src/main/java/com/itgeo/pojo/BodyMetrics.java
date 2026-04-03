package com.itgeo.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private LocalDate recordDate;
    private BigDecimal weight;
    private BigDecimal bodyFat;
    private BigDecimal sleepHours;
    private String fatigueLevel;
    private String note;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
