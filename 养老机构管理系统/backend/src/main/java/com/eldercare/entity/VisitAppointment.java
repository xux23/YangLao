package com.eldercare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 探访预约实体
 */
@Data
@TableName("visit_appointment")
public class VisitAppointment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 老人 ID */
    private Long elderId;

    /** 预约家属用户 ID */
    private Long familyId;

    /** 探访日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;

    /** 探访时段（如 上午9:00-11:00） */
    private String visitTime;

    /** 探访人数 */
    private Integer persons;

    /** 状态：0 待审核 / 1 已通过 / 2 已驳回 / 3 已完成 */
    private Integer status;

    /** 审核意见（驳回必填） */
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 老人姓名（非数据库字段，查询时补充） */
    @TableField(exist = false)
    private String elderName;

    /** 家属姓名（非数据库字段，查询时补充） */
    @TableField(exist = false)
    private String familyName;
}