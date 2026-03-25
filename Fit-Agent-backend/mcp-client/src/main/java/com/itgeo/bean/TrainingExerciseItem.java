package com.itgeo.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingExerciseItem {
    private String name;
    private Integer sets;
    private Integer reps;
    private BigDecimal weight;
}
