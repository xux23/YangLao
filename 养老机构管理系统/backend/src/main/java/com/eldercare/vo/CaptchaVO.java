package com.eldercare.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图形验证码返回对象：编号 + Base64 图片
 */
@Data
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码编号（登录时回传，用于从 Redis 定位验证码文本） */
    private String captchaId;

    /** 验证码图片（data:image/png;base64,... 前端直接作为 img 的 src） */
    private String image;
}
