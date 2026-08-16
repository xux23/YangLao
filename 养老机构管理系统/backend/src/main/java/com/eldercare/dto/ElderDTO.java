package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 老人新增/修改请求参数
 */
@Data
public class ElderDTO {

    @NotBlank(message = "老人姓名不能为空")
    private String name;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    /** 身份证号：新增时必填且唯一，修改时不传则保持不变 */
    private String idCard;

    private String phone;

    private String emergencyContact;

    private String emergencyPhone;

    /** 健康概况 */
    private String healthSummary;

    /** 关联家属账号 ID（可为空） */
    private Long familyId;
}