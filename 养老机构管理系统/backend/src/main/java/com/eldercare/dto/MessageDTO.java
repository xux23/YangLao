package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 家属发表留言请求参数
 */
@Data
public class MessageDTO {

    @NotNull(message = "老人不能为空")
    private Long elderId;

    @NotBlank(message = "留言内容不能为空")
    private String content;
}