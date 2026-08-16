package com.eldercare.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.entity.SysLog;

/**
 * 操作日志业务接口（仅管理员）
 */
public interface LogService {

    /**
     * 日志分页查询，支持操作人、时间段筛选
     */
    Page<SysLog> pageLog(int page, int size, String username, String startTime, String endTime);
}