package com.eldercare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 护理记录实体（护理计划与执行记录合一）
 */
@Data
@TableName("care_record")
public class CareRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 老人 ID */
    private Long elderId;

    /** 护理项目（如翻身、喂饭） */
    private String planName;

    /** 频次说明 */
    private String planFrequency;

    /** 护理内容 */
    private String careContent;

    /** 执行护理人员 ID */
    private Long nurseId;

    /** 执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime careTime;

    /** 交接班备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 老人姓名（非数据库字段，查询时补充） */
    @TableField(exist = false)
    private String elderName;

    /** 护理人员姓名（非数据库字段，查询时补充） */
    @TableField(exist = false)
    private String nurseName;
}