package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用药计划新增请求参数
 */
@Data
public class MedicinePlanDTO {

    @NotNull(message = "老人不能为空")
    private Long elderId;

    @NotBlank(message = "药名不能为空")
    private String medicineName;

    /** 剂量说明 */
    private String dosage;

    /** 服药时间点列表，如 ["08:00", "14:00"]，每个时间点生成一行当日任务 */
    @NotEmpty(message = "至少填写一个服药时间点")
    private List<String> times;
}