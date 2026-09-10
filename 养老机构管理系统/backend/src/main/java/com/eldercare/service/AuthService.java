package com.eldercare.service;

import com.eldercare.dto.LoginDTO;
import com.eldercare.dto.PasswordDTO;
import com.eldercare.entity.SysUser;
import com.eldercare.vo.CaptchaVO;
import com.eldercare.vo.LoginVO;

/**
 * 认证业务接口
 */
public interface AuthService {

    /**
     * 生成图形验证码：图片 Base64 返回，验证码文本存 Redis
     */
    CaptchaVO createCaptcha();

    /**
     * 登录：依次校验账号锁定 → 验证码 → 账号密码，签发 JWT
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前登录用户信息
     */
    SysUser getCurrentUser();

    /**
     * 修改密码：验证原密码，新密码 BCrypt 加密保存
     */
    void changePassword(PasswordDTO dto);

    /**
     * 安全退出：将当前令牌写入 Redis 黑名单，剩余有效期内不可再使用
     */
    void logout(String token);
}
