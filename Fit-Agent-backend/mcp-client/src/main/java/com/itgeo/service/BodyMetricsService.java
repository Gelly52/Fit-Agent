package com.itgeo.service;

import com.itgeo.bean.BodyMetricsLogRequest;
import com.itgeo.bean.DateSummaryItem;

import java.util.List;

/**
 * 身体指标服务接口。
 */
public interface BodyMetricsService {
    /**
     * 记录指定用户的身体指标。
     *
     * @param userId 用户主键
     * @param request 身体指标请求体
     */
    void logBodyMetrics(Long userId, BodyMetricsLogRequest request);

    /**
     * 查询指定用户最近的身体指标摘要。
     *
     * @param userId 用户主键
     * @param limit 返回条数
     * @return 日期摘要列表
     */
    List<DateSummaryItem> getRecentBodyMetrics(Long userId, Integer limit);
}
