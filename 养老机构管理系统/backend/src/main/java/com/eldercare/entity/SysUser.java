package com.eldercare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（admin / nurse / family 三类角色共用一张表）
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（唯一） */
    private String username;

    /** 密码密文，序列化时忽略，任何接口不返回密码 */
    @JsonIgnore
    private String password;

    /** 姓名 */
    private String realName;

    /** 角色：admin 管理员 / nurse 护理人员 / family 家属 */
    private String role;

    private String phone;

    /** 状态：1 启用 / 0 禁用 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}