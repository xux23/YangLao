package com.eldercare.controller;

import com.eldercare.aspect.OperLog;
import com.eldercare.common.Result;
import com.eldercare.dto.MedicinePlanDTO;
import com.eldercare.entity.MedicinePlan;
import com.eldercare.security.RequireRole;
import com.eldercare.service.MedicinePlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药计划/任务接口
 */
@RestController
@RequestMapping("/api")
public class MedicineController {

    @Autowired
    private MedicinePlanService medicinePlanService;

    /**
     * 录入用药计划：为当天每个服药时间点生成一行任务（护理人员）
     */
    @OperLog("新增用药计划")
    @PostMapping("/medicine-plans")
    @RequireRole("nurse")
    public Result<Void> addPlan(@Valid @RequestBody MedicinePlanDTO dto) {
        medicinePlanService.addPlan(dto);
        return Result.success("用药计划已保存，今日任务已生成", null);
    }

    /**
     * 在用药计划列表（管理员、护理人员）
     */
    @GetMapping("/medicine-plans")
    @RequireRole({"admin", "nurse"})
    public Result<List<MedicinePlan>> listPlans(@RequestParam(required = false) Long elderId) {
        return Result.success(medicinePlanService.listPlans(elderId));
    }

    /**
     * 停用用药计划（护理人员）
     */
    @OperLog("停用用药计划")
    @PutMapping("/medicine-plans/{id}/disable")
    @RequireRole("nurse")
    public Result<Void> disablePlan(@PathVariable Long id) {
        medicinePlanService.disablePlan(id);
        return Result.success("用药计划已停用", null);
    }

    /**
     * 查询某日用药任务（查询时自动生成当日任务并做逾期扫描）
     */
    @GetMapping("/medicine-tasks")
    @RequireRole({"admin", "nurse"})
    public Result<List<MedicinePlan>> listTasks(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long elderId) {
        return Result.success(medicinePlanService.listTasks(date, elderId));
    }

    /**
     * 任务确认执行（护理人员）
     */
    @OperLog("确认用药")
    @PutMapping("/medicine-tasks/{id}/complete")
    @RequireRole("nurse")
    public Result<MedicinePlan> completeTask(@PathVariable Long id) {
        return Result.success("已确认执行", medicinePlanService.completeTask(id));
    }

    /**
     * 逾期任务列表（护理人员、管理员）
     */
    @GetMapping("/medicine-tasks/overdue")
    @RequireRole({"admin", "nurse"})
    public Result<List<MedicinePlan>> overdueTasks(@RequestParam(required = false) Long elderId) {
        return Result.success(medicinePlanService.listOverdueTasks(elderId));
    }
}