package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.BusinessException;
import com.eldercare.entity.SysLog;
import com.eldercare.mapper.SysLogMapper;
import com.eldercare.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 操作日志业务实现
 */
@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private SysLogMapper sysLogMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<SysLog> pageLog(int page, int size, String username, String startTime, String endTime) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysLog::getUsername, username);

        try {
            if (StringUtils.hasText(startTime)) {
                LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
                wrapper.ge(SysLog::getCreateTime, start);
            }
            if (StringUtils.hasText(endTime)) {
                LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
                wrapper.le(SysLog::getCreateTime, end);
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
        wrapper.orderByDesc(SysLog::getId);
        return sysLogMapper.selectPage(new Page<>(page, size), wrapper);
    }
}