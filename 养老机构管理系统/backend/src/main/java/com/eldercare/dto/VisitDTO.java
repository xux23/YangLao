package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 探访预约提交请求参数
 */
@Data
public class VisitDTO {

    @NotNull(message = "老人不能为空")
    private Long elderId;

    @NotNull(message = "探访日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;

    @NotBlank(message = "探访时段不能为空")
    private String visitTime;

    @NotNull(message = "探访人数不能为空")
    @Min(value = 1, message = "探访人数至少 1 人")
    private Integer persons;

    private String remark;
}