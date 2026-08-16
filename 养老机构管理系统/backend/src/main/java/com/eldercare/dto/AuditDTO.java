package com.eldercare.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 探访预约审核请求参数
 */
@Data
public class AuditDTO {

    /** 1 通过 / 2 驳回 */
    @NotNull(message = "审核结果不能为空")
    private Integer status;

    /** 驳回原因（驳回时必填） */
    private String auditRemark;
}