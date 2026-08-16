package com.eldercare.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.aspect.OperLog;
import com.eldercare.common.Result;
import com.eldercare.dto.HealthRecordDTO;
import com.eldercare.entity.HealthRecord;
import com.eldercare.security.RequireRole;
import com.eldercare.service.HealthRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 健康体征记录接口
 */
@RestController
@RequestMapping("/api/health-records")
public class HealthRecordController {

    @Autowired
    private HealthRecordService healthRecordService;

    /**
     * 体征记录分页查询（家属仅能查关联老人）
     */
    @GetMapping
    @RequireRole({"admin", "nurse", "family"})
    public Result<Page<HealthRecord>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long elderId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(
                healthRecordService.pageHealthRecord(page, size, elderId, startTime, endTime));
    }

    /**
     * 新增体征记录（护理人员）
     */
    @OperLog("新增体征记录")
    @PostMapping
    @RequireRole({"admin", "nurse"})
    public Result<Void> add(@Valid @RequestBody HealthRecordDTO dto) {
        healthRecordService.addHealthRecord(dto);
        return Result.success("新增成功", null);
    }

    /**
     * 修改体征记录（护理人员）
     */
    @OperLog("修改体征记录")
    @PutMapping("/{id}")
    @RequireRole({"admin", "nurse"})
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody HealthRecordDTO dto) {
        healthRecordService.updateHealthRecord(id, dto);
        return Result.success("修改成功", null);
    }

    /**
     * 删除体征记录（护理人员）
     */
    @OperLog("删除体征记录")
    @DeleteMapping("/{id}")
    @RequireRole({"admin", "nurse"})
    public Result<Void> delete(@PathVariable Long id) {
        healthRecordService.deleteHealthRecord(id);
        return Result.success("删除成功", null);
    }
}