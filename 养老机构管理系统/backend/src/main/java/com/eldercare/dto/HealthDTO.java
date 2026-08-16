package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 健康档案（健康概况）修改请求参数
 */
@Data
public class HealthDTO {

    /** 健康概况：病史、过敏史、用药禁忌等 */
    @NotBlank(message = "健康概况不能为空")
    private String healthSummary;
}