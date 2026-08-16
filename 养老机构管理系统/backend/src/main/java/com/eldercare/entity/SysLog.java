package com.eldercare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体（AOP 切面自动写入，只增不删，仅管理员可查）
 */
@Data
@TableName("sys_log")
public class SysLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人用户 ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作描述（如 新增老人） */
    private String operation;

    /** 请求方法 + 路径 */
    private String method;

    /** 请求参数（截断 500 字符） */
    private String params;

    /** 客户端 IP */
    private String ip;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}