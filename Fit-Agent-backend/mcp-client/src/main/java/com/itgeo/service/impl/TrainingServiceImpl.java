package com.itgeo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itgeo.bean.DateSummaryItem;
import com.itgeo.bean.TrainingExerciseItem;
import com.itgeo.bean.TrainingLogRequest;
import com.itgeo.mapper.TrainingExerciseMapper;
import com.itgeo.mapper.TrainingLogMapper;
import com.itgeo.pojo.TrainingExercise;
import com.itgeo.pojo.TrainingLog;
import com.itgeo.service.TrainingService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class) // 事务回滚，训练主表和训练详情表同时回滚
public class TrainingServiceImpl implements TrainingService {

    @Resource
    private TrainingLogMapper trainingLogMapper;

    @Resource
    private TrainingExerciseMapper trainingExerciseMapper;

    @Override
    public void logTraining(Long userId, TrainingLogRequest request) {
        // 1.校验参数
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getDate() == null || request.getDate().isBlank()) {
            throw new IllegalArgumentException("训练日期不能为空");
        }
        if (request.getExercises() == null || request.getExercises().isEmpty()) {
            throw new IllegalArgumentException("训练动作不能为空");
        }
        // 过滤空动作名
        List<TrainingExerciseItem> validExercises = request.getExercises().stream()
                .filter(item -> item != null && item.getName() != null && !item.getName().trim().isBlank())
                .toList();
        if (validExercises.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个有效训练动作");
        }

        // 2.解析日期
        LocalDate trainingDate = LocalDate.parse(request.getDate());

        // 3.记录总训练量 sets * reps * weight
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (TrainingExerciseItem item : validExercises) {
            int sets = item.getSets() == null || item.getSets() <= 0 ? 1 : item.getSets();
            int reps = item.getReps() == null || item.getReps() <= 0 ? 1 : item.getReps();
            BigDecimal weight = item.getWeight() == null ? BigDecimal.ZERO : item.getWeight();

            BigDecimal volume = weight.multiply(BigDecimal.valueOf((long) sets * reps));
            totalVolume = totalVolume.add(volume);
        }
        // 4.生成训练摘要
        String summary = buildTrainingSummary(validExercises);

        // 5.判断当天记录是否存在
        TrainingLog existing = trainingLogMapper.selectOne(
                new LambdaQueryWrapper<TrainingLog>()
                        .eq(TrainingLog::getUserId, userId)
                        .eq(TrainingLog::getTrainingDate, trainingDate)
                        .last("limit 1")
        );
        // 6.1 如果存在，更新记录
        Long trainingLogId = null;
        if (existing != null) {
            existing.setSummary(summary);
            existing.setTotalVolume(totalVolume);
            existing.setSource("manual");
            trainingLogMapper.updateById(existing);
            trainingLogId = existing.getId();
            trainingExerciseMapper.delete(
                    new LambdaQueryWrapper<TrainingExercise>()
                            .eq(TrainingExercise::getTrainingLogId, trainingLogId)
            );
        } else {
            // 6.2 如果不存在，创建新记录
            TrainingLog log = new TrainingLog();
            log.setUserId(userId);
            log.setTrainingDate(trainingDate);
            log.setSummary(summary);
            log.setPrimaryMuscleGroup(null);
            log.setTotalVolume(totalVolume);
            log.setSource("manual");
            trainingLogMapper.insert(log);
            trainingLogId = log.getId();
        }

        // 7.记录训练详情
        for (int i = 0; i < validExercises.size(); i++) {
            TrainingExerciseItem item = validExercises.get(i);

            TrainingExercise exercise = new TrainingExercise();
            exercise.setTrainingLogId(trainingLogId);
            exercise.setExerciseName(item.getName().trim());
            exercise.setSets(item.getSets() == null || item.getSets() <= 0 ? 1 : item.getSets());
            exercise.setReps(item.getReps() == null || item.getReps() <= 0 ? 1 : item.getReps());
            exercise.setWeight(item.getWeight() == null ? BigDecimal.ZERO : item.getWeight());
            exercise.setOrderNum(i + 1);
            exercise.setEstimatedMuscleGroup(null);

            trainingExerciseMapper.insert(exercise);
        }


    }

    // 生成训练总结摘要
    private String buildTrainingSummary(List<TrainingExerciseItem> exercises) {
        return exercises.stream()
                .limit(3)
                .map(item -> {
                    int sets = item.getSets() == null || item.getSets() <= 0 ? 1 : item.getSets();
                    int reps = item.getReps() == null || item.getReps() <= 0 ? 1 : item.getReps();
                    BigDecimal weight = item.getWeight() == null ? BigDecimal.ZERO : item.getWeight();
                    return item.getName().trim() + " " + sets + "组x" + reps + "次，" +
                            weight.stripTrailingZeros().toPlainString() + "kg";
                })
                .collect(Collectors.joining("；"));
    }

    @Override
    public List<DateSummaryItem> getRecentTraining(Long userId, Integer limit) {
        // 1.校验参数
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        // 2.处理limit
        int safeLimit = (limit == null || limit <= 0) ? 5 : Math.min(limit, 20);
        // 3.查询数据库
        List<TrainingLog> logs = trainingLogMapper.selectList(
                new LambdaQueryWrapper<TrainingLog>()
                        .eq(TrainingLog::getUserId, userId)
                        .orderByDesc(TrainingLog::getTrainingDate)
                        .last("limit " + safeLimit)
        );
        // 4.返回结果
        return logs.stream()
                .map(log -> new DateSummaryItem(
                        log.getTrainingDate() == null ? null : log.getTrainingDate().toString(),
                        log.getSummary()))
                .collect(Collectors.toList());
    }
}
