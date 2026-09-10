package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码编号（captcha 接口返回，启用验证码时必填） */
    private String captchaId;

    /** 用户输入的验证码文本（启用验证码时必填） */
    private String captchaCode;
}