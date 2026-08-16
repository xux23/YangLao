package com.eldercare.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.aspect.OperLog;
import com.eldercare.common.Result;
import com.eldercare.dto.AuditDTO;
import com.eldercare.dto.VisitDTO;
import com.eldercare.entity.VisitAppointment;
import com.eldercare.security.RequireRole;
import com.eldercare.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 探访预约接口：家属提交，管理员/护理人员审核
 */
@RestController
@RequestMapping("/api/visits")
public class VisitController {

    @Autowired
    private VisitService visitService;

    /**
     * 家属提交探访预约
     */
    @OperLog("提交探访预约")
    @PostMapping
    @RequireRole("family")
    public Result<Void> add(@Valid @RequestBody VisitDTO dto) {
        visitService.addVisit(dto);
        return Result.success("预约已提交，等待机构审核", null);
    }

    /**
     * 预约分页查询：家属只看自己的，机构看全部
     */
    @GetMapping
    public Result<Page<VisitAppointment>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long elderId) {
        return Result.success(visitService.pageVisit(page, size, status, elderId));
    }

    /**
     * 预约审核：通过 / 驳回（管理员、护理人员）
     */
    @OperLog("探访审核")
    @PutMapping("/{id}/audit")
    @RequireRole({"admin", "nurse"})
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        visitService.auditVisit(id, dto);
        return Result.success("审核完成", null);
    }

    /**
     * 标记探访完成（管理员、护理人员）
     */
    @OperLog("标记探访完成")
    @PutMapping("/{id}/finish")
    @RequireRole({"admin", "nurse"})
    public Result<Void> finish(@PathVariable Long id) {
        visitService.finishVisit(id);
        return Result.success("已标记为完成", null);
    }
}