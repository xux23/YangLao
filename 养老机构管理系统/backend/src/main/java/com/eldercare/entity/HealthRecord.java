package com.eldercare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康体征记录实体
 */
@Data
@TableName("health_record")
public class HealthRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 老人 ID */
    private Long elderId;

    /** 血压（如 128/82） */
    private String bloodPressure;

    /** 心率（次/分） */
    private Integer heartRate;

    /** 体温（℃） */
    private BigDecimal temperature;

    /** 血糖（mmol/L） */
    private BigDecimal bloodSugar;

    /** 测量时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    /** 录入人 ID */
    private Long recorderId;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 老人姓名（非数据库字段，查询时补充） */
    @TableField(exist = false)
    private String elderName;
}