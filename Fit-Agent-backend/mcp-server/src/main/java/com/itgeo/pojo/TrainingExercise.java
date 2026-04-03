package com.itgeo.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ToString
@TableName("t_training_exercise")
public class TrainingExercise {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long trainingLogId;
    private String exerciseName;
    private Integer sets;
    private Integer reps;
    private BigDecimal weight;
    private Integer orderNum;
    private String estimatedMuscleGroup;
    private LocalDateTime createdAt;
}
