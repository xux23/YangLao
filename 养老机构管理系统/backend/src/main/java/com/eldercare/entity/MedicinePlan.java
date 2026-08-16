package com.eldercare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用药计划实体：一行 = 某老人某天某个时间点的一次用药任务。
 * 计划与任务共用一张表，任务按需生成。
 */
@Data
@TableName("medicine_plan")
public class MedicinePlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 老人 ID */
    private Long elderId;

    /** 药名 */
    private String medicineName;

    /** 剂量（如每次1片） */
    private String dosage;

    /** 服药日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    /** 服药时间点 */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime planTime;

    /** 状态：0 待执行 / 1 已执行 / 2 已逾期 */
    private Integer status;

    /** 确认执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime confirmTime;

    /** 是否停用：0 正常 / 1 已停用（停用后不再按需生成新任务） */
    private Integer disabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 老人姓名（非数据库字段，查询时补充） */
    @TableField(exist = false)
    private String elderName;
}