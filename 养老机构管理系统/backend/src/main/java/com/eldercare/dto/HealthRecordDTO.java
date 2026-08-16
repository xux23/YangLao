package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康体征记录新增/修改请求参数
 */
@Data
public class HealthRecordDTO {

    @NotNull(message = "老人不能为空")
    private Long elderId;

    /** 血压（如 128/82） */
    private String bloodPressure;

    /** 心率（次/分） */
    private Integer heartRate;

    /** 体温（℃） */
    private BigDecimal temperature;

    /** 血糖（mmol/L） */
    private BigDecimal bloodSugar;

    /** 测量时间（不传默认当前时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    private String remark;
}