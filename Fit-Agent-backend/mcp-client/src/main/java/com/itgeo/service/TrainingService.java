package com.itgeo.service;

import com.itgeo.bean.DateSummaryItem;
import com.itgeo.bean.TrainingLogRequest;

import java.util.List;

public interface TrainingService {
    // 记录训练日志
    void logTraining(Long userId, TrainingLogRequest trainingLogRequest);

    // 获取最近训练日志
    List<DateSummaryItem> getRecentTraining(Long userId, Integer limit);
}
