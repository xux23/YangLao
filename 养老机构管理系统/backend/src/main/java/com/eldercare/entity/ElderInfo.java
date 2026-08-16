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
 * 老人信息实体（业务主表，房间/床位用字段保存，不单独建表）
 */
@Data
@TableName("elder_info")
public class ElderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 性别：1 男 / 2 女 */
    private Integer gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /** 身份证号（唯一） */
    private String idCard;

    private String phone;

    /** 紧急联系人 */
    private String emergencyContact;

    /** 紧急联系电话 */
    private String emergencyPhone;

    /** 房间号 */
    private String roomNo;

    /** 床位号 */
    private String bedNo;

    /** 健康概况（病史、过敏史等） */
    private String healthSummary;

    /** 状态：1 在住 / 0 已退住 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkinTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkoutTime;

    /** 关联家属账号 ID（sys_user.id） */
    private Long familyId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 关联家属姓名（非数据库字段，查询时补充） */
    @TableField(exist = false)
    private String familyName;
}