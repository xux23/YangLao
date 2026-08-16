package com.eldercare.aspect;

import com.eldercare.entity.SysLog;
import com.eldercare.mapper.SysLogMapper;
import com.eldercare.security.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面：拦截标注了 @OperLog 的方法，自动把操作记录写入 sys_log 表。
 * 采用 Around 通知：方法执行成功后记录（操作失败不记录）。
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysLogMapper sysLogMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_PARAMS_LENGTH = 500;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            saveLog(joinPoint, operLog.value());
        } catch (Exception e) {
            // 日志记录失败不能影响正常业务，只打印错误
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 组装并保存一条操作日志
     */
    private void saveLog(ProceedingJoinPoint joinPoint, String operation) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        SysLog sysLog = new SysLog();
        sysLog.setUserId(UserContext.getUserId());
        sysLog.setUsername(UserContext.get() != null ? UserContext.get().getUsername() : null);
        sysLog.setOperation(operation);
        sysLog.setMethod(request.getMethod() + " " + request.getRequestURI());
        sysLog.setParams(buildParams(joinPoint));
        sysLog.setIp(getIpAddress(request));

        sysLogMapper.insert(sysLog);
    }

    /**
     * 把方法参数序列化成 JSON，并截断到 500 字符；
     * 请求/响应对象不需要记录，直接跳过
     */
    private String buildParams(ProceedingJoinPoint joinPoint) {
        try {
            String params = Arrays.stream(joinPoint.getArgs())
                    .filter(arg -> arg != null
                            && !(arg instanceof HttpServletRequest)
                            && !(arg instanceof HttpServletResponse))
                    .map(arg -> {
                        try {
                            return OBJECT_MAPPER.writeValueAsString(arg);
                        } catch (Exception e) {
                            // 单个参数序列化失败不影响整体日志
                            return "";
                        }
                    })
                    .collect(Collectors.joining(","));
            if (params.length() > MAX_PARAMS_LENGTH) {
                params = params.substring(0, MAX_PARAMS_LENGTH);
            }
            return params;
        } catch (Exception e) {
            return "";
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}