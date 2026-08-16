package com.eldercare.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 的写操作方法上，
 * 例如 @OperLog("新增老人")，由 LogAspect 切面自动记录日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /**
     * 操作描述，如"新增老人""探访审核"
     */
    String value();
}