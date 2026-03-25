package com.itgeo.controller;

import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.TrainingLogRequest;
import com.itgeo.service.TrainingService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/training")
public class TrainingController {

    @Resource
    private TrainingService trainingService;

    @PostMapping("/log")
    public LeeResult logTraining(@RequestBody TrainingLogRequest request) {
        try {
            Long userId = UserContextHolder.getRequired().getUserId();
            trainingService.logTraining(userId, request);
            return LeeResult.ok();
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("保存训练记录失败", e);
            return LeeResult.errorException("保存训练记录失败");
        }
    }

    @GetMapping("/recent")
    public LeeResult getRecentTraining(@RequestParam(required = false) Integer limit) {
        try {
            Long userId = UserContextHolder.getRequired().getUserId();
            return LeeResult.ok(trainingService.getRecentTraining(userId, limit));
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("查询最近训练记录失败", e);
            return LeeResult.errorException("查询最近训练记录失败");
        }
    }
}
