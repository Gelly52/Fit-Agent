package com.itgeo.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ToString
@TableName("t_training_exercise")
public class TrainingExercise {
    private Long id;
    @TableField("training_log_id")
    private Long trainingLogId;
    @TableField("exercise_name")
    private String exerciseName;
    private Integer sets;
    private Integer reps;
    private BigDecimal weight;
    @TableField("order_num")
    private Integer orderNum;
    @TableField("estimated_muscle_group")
    private String estimatedMuscleGroup;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
