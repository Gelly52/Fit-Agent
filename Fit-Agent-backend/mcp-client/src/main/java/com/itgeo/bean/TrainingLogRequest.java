package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 训练日志记录请求。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainingLogRequest {
    /** 训练日期，格式 yyyy-MM-dd。 */
    private String date;
    /** 本次训练的动作列表。 */
    private List<TrainingExerciseItem> exercises;
}
