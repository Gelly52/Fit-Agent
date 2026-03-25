package com.itgeo.service;

import com.itgeo.bean.BodyMetricsLogRequest;
import com.itgeo.bean.DateSummaryItem;

import java.util.List;

public interface BodyMetricsService {
    // 记录身体指标日志
    void logBodyMetrics(Long userId, BodyMetricsLogRequest request);

    // 获取最近身体指标日志
    List<DateSummaryItem> getRecentBodyMetrics(Long userId, Integer limit);
}
