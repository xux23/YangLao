package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 留言回复请求参数
 */
@Data
public class ReplyDTO {

    @NotBlank(message = "回复内容不能为空")
    private String reply;
}