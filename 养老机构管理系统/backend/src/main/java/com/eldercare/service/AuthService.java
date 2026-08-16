package com.eldercare.service;

import com.eldercare.dto.PasswordDTO;
import com.eldercare.entity.SysUser;
import com.eldercare.vo.LoginVO;

/**
 * 认证业务接口
 */
public interface AuthService {

    /**
     * 登录：校验账号密码，签发 JWT
     */
    LoginVO login(String username, String password);

    /**
     * 获取当前登录用户信息
     */
    SysUser getCurrentUser();

    /**
     * 修改密码：验证原密码，新密码 BCrypt 加密保存
     */
    void changePassword(PasswordDTO dto);
}