package com.eldercare.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.Result;
import com.eldercare.entity.SysLog;
import com.eldercare.security.RequireRole;
import com.eldercare.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口（仅管理员查看）
 */
@RestController
@RequestMapping("/api/logs")
@RequireRole("admin")
public class LogController {

    @Autowired
    private LogService logService;

    /**
     * 日志分页查询，支持操作人、时间段筛选
     */
    @GetMapping
    public Result<Page<SysLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(logService.pageLog(page, size, username, startTime, endTime));
    }
}