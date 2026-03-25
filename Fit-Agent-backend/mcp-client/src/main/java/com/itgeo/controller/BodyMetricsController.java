package com.itgeo.controller;

import com.itgeo.auth.UserContextHolder;
import com.itgeo.bean.BodyMetricsLogRequest;
import com.itgeo.service.BodyMetricsService;
import com.itgeo.utils.LeeResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/body-metrics")
public class BodyMetricsController {

    @Resource
    private BodyMetricsService bodyMetricsService;

    @PostMapping("/log")
    public LeeResult logBodyMetrics(@RequestBody BodyMetricsLogRequest request) {
        try {
            Long userId = UserContextHolder.getRequired().getUserId();
            bodyMetricsService.logBodyMetrics(userId, request);
            return LeeResult.ok();
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("保存身体指标失败", e);
            return LeeResult.errorException("保存身体指标失败");
        }
    }

    @GetMapping("/recent")
    public LeeResult getRecentBodyMetrics(@RequestParam(required = false) Integer limit) {
        try {
            Long userId = UserContextHolder.getRequired().getUserId();
            return LeeResult.ok(bodyMetricsService.getRecentBodyMetrics(userId, limit));
        } catch (IllegalArgumentException e) {
            return LeeResult.errorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("查询最近身体指标失败", e);
            return LeeResult.errorException("查询最近身体指标失败");
        }
    }
}
