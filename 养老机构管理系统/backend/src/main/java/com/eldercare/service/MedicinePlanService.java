package com.eldercare.service;

import com.eldercare.dto.MedicinePlanDTO;
import com.eldercare.entity.MedicinePlan;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药计划/任务业务接口（核心：任务按需生成 + 逾期扫描）
 */
public interface MedicinePlanService {

    /**
     * 录入用药计划：为当天每个时间点生成一行任务
     */
    void addPlan(MedicinePlanDTO dto);

    /**
     * 在用药计划列表（取老人最近一天的任务行）
     */
    List<MedicinePlan> listPlans(Long elderId);

    /**
     * 停用计划：删除该老人今天及以后的任务行，历史行保留并标记停用
     */
    void disablePlan(Long id);

    /**
     * 查询某日用药任务：
     * 1. 先执行逾期扫描（把过期未执行的任务标记为逾期）；
     * 2. 当天没有任务行时，按最近一天的任务复制生成。
     */
    List<MedicinePlan> listTasks(LocalDate date, Long elderId);

    /**
     * 确认执行：任务状态置为已执行并记录确认时间
     */
    MedicinePlan completeTask(Long id);

    /**
     * 逾期任务列表（提醒护理人员补服/补录）
     */
    List<MedicinePlan> listOverdueTasks(Long elderId);
}