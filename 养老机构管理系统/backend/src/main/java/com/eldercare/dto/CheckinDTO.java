package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 入住登记请求参数
 */
@Data
public class CheckinDTO {

    @NotBlank(message = "房间号不能为空")
    private String roomNo;

    @NotBlank(message = "床位号不能为空")
    private String bedNo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkinTime;
}