package com.eldercare.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 退住登记请求参数
 */
@Data
public class CheckoutDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkoutTime;
}