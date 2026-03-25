package com.itgeo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itgeo.bean.BodyMetricsLogRequest;
import com.itgeo.bean.DateSummaryItem;
import com.itgeo.mapper.BodyMetricsMapper;
import com.itgeo.pojo.BodyMetrics;
import com.itgeo.service.BodyMetricsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class BodyMetricsServiceImpl implements BodyMetricsService {

    @Resource
    private BodyMetricsMapper bodyMetricsMapper;

    @Override
    public void logBodyMetrics(Long userId, BodyMetricsLogRequest request) {
        // 1. 校验参数
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getDate() == null || request.getDate().isBlank()) {
            throw new IllegalArgumentException("记录日期不能为空");
        }
        if (request.getWeight() == null && request.getBodyFat() == null) {
            throw new IllegalArgumentException("体重和体脂至少填写一项");
        }
        // 2. 解析日期格式
        LocalDate recordDate = LocalDate.parse(request.getDate());
        // 3. 构建身体指标摘要
        String summary = buildBodyMetricsSummary(request);
        // 4.查询当天是否已有记录
        BodyMetrics existing = bodyMetricsMapper.selectOne(
                new LambdaQueryWrapper<BodyMetrics>()
                        .eq(BodyMetrics::getUserId, userId)
                        .eq(BodyMetrics::getRecordDate, recordDate)
                        .last("limit 1")
        );
        // 5.如果存在记录，更新记录
        if (existing != null) {
            existing.setWeight(request.getWeight());
            existing.setBodyFat(request.getBodyFat());
            existing.setSleepHours(request.getSleep());
            existing.setFatigueLevel(blankToNull(request.getFatigue()));
            existing.setNote(blankToNull(request.getNote()));
            existing.setSummary(summary);
            bodyMetricsMapper.updateById(existing);
        } else {
            // 6.如果不存在记录，插入新记录
            BodyMetrics entity = new BodyMetrics();
            entity.setUserId(userId);
            entity.setRecordDate(recordDate);
            entity.setWeight(request.getWeight());
            entity.setBodyFat(request.getBodyFat());
            entity.setSleepHours(request.getSleep());
            entity.setFatigueLevel(blankToNull(request.getFatigue()));
            entity.setNote(blankToNull(request.getNote()));
            entity.setSummary(summary);
            bodyMetricsMapper.insert(entity);
        }

    }

    @Override
    public List<DateSummaryItem> getRecentBodyMetrics(Long userId, Integer limit) {
        // 1.校验参数
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        // 2.处理limit
        int safeLimit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
        // 3.查询数据库
        List<BodyMetrics> records = bodyMetricsMapper.selectList(
                new LambdaQueryWrapper<BodyMetrics>()
                        .eq(BodyMetrics::getUserId, userId)
                        .orderByDesc(BodyMetrics::getRecordDate)
                        .last("limit " + safeLimit)
        );
        // 4.返回结果
        return records.stream()
                .map(record -> new DateSummaryItem(
                        record.getRecordDate() == null ? null : record.getRecordDate().toString(),
                        record.getSummary()))
                .collect(Collectors.toList());
    }

    // 构建身体指标摘要
    private String buildBodyMetricsSummary(BodyMetricsLogRequest request) {
        List<String> parts = new java.util.ArrayList<>();

        if (request.getWeight() != null) {
            parts.add("体重 " + request.getWeight().stripTrailingZeros().toPlainString() + "kg");
        }
        if (request.getBodyFat() != null) {
            parts.add("体脂 " + request.getBodyFat().stripTrailingZeros().toPlainString() + "%");
        }
        if (request.getSleep() != null) {
            parts.add("睡眠 " + request.getSleep().stripTrailingZeros().toPlainString() + "小时");
        }
        if (request.getFatigue() != null && !request.getFatigue().isBlank()) {
            parts.add("疲劳度 " + request.getFatigue().trim());
        }

        if (parts.isEmpty()) {
            return "暂无身体指标";
        }

        return String.join("，", parts);
    }

    // 处理空字符串
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
