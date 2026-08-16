package com.eldercare.controller;

import com.eldercare.common.Result;
import com.eldercare.security.RequireRole;
import com.eldercare.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统计接口：首页数据看板（ECharts 数据源）
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    /**
     * 看板总览：老人总数、在住数、入住率、今日护理/探访数、逾期任务数
     */
    @GetMapping("/overview")
    @RequireRole("admin")
    public Result<Map<String, Object>> overview() {
        return Result.success(statsService.getOverview());
    }

    /**
     * 老人年龄分布（饼图）
     */
    @GetMapping("/age-distribution")
    @RequireRole("admin")
    public Result<Map<String, Object>> ageDistribution() {
        return Result.success(statsService.getAgeDistribution());
    }

    /**
     * 近 N 天护理/探访数量趋势（折线图）
     */
    @GetMapping("/activity-trend")
    @RequireRole("admin")
    public Result<Map<String, Object>> activityTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.success(statsService.getActivityTrend(days));
    }

    /**
     * 某老人体征趋势（折线图）。
     * 该接口同时开放给管理员/护理人员和家属（家属仅限关联老人），
     * 因为家属健康档案页面需要展示体征趋势图。
     */
    @GetMapping("/elder/{id}/health-trend")
    @RequireRole({"admin", "nurse", "family"})
    public Result<Map<String, Object>> healthTrend(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "30") int days,
                                                   @RequestParam(defaultValue = "bloodPressure") String metric) {
        return Result.success(statsService.getHealthTrend(id, days, metric));
    }
}