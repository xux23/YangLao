package com.eldercare.service;

import java.util.List;
import java.util.Map;

/**
 * 统计看板业务接口（首页数据看板，供 ECharts 使用）
 */
public interface StatsService {

    /**
     * 看板总览数据：老人总数、在住数、入住率、今日护理/探访数、逾期任务数
     */
    Map<String, Object> getOverview();

    /**
     * 老人年龄分布
     */
    Map<String, Object> getAgeDistribution();

    /**
     * 近 N 天护理/探访数量趋势（缺数日期补 0）
     */
    Map<String, Object> getActivityTrend(int days);

    /**
     * 某老人近 N 天体征趋势
     */
    Map<String, Object> getHealthTrend(Long elderId, int days, String metric);

    /**
     * 体征趋势中允许查询的指标
     */
    List<String> getSupportedMetrics();
}