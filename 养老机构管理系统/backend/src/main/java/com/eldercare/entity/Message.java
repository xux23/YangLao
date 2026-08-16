package com.eldercare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家属留言实体
 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 老人 ID */
    private Long elderId;

    /** 家属用户 ID */
    private Long familyId;

    /** 留言内容 */
    private String content;

    /** 回复内容 */
    private String reply;

    /** 回复时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime replyTime;

    /** 状态：0 未回复 / 1 已回复 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 老人姓名（非数据库字段，查询时补充） */
    @TableField(exist = false)
    private String elderName;

    /** 家属姓名（非数据库字段，查询时补充） */
    @TableField(exist = false)
    private String familyName;
}