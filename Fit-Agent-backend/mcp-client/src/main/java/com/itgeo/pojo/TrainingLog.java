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
@TableName("t_training_log")
public class TrainingLog {
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("training_date")
    private LocalDate trainingDate;
    private String summary;
    @TableField("primary_muscle_group")
    private String primaryMuscleGroup;
    @TableField("total_volume")
    private BigDecimal totalVolume;
    private String source;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

