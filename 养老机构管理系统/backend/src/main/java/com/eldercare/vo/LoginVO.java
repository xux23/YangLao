package com.eldercare.vo;

import com.eldercare.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功返回对象：令牌 + 用户信息
 */
@Data
@AllArgsConstructor
public class LoginVO {

    /** JWT 令牌 */
    private String token;

    /** 用户信息（不含密码） */
    private SysUser user;
}