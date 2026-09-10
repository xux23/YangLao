package com.eldercare.controller;

import com.eldercare.aspect.OperLog;
import com.eldercare.common.Result;
import com.eldercare.dto.LoginDTO;
import com.eldercare.dto.PasswordDTO;
import com.eldercare.entity.SysUser;
import com.eldercare.service.AuthService;
import com.eldercare.vo.CaptchaVO;
import com.eldercare.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口：验证码、登录、当前用户信息、修改密码、安全退出
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 图形验证码（放行路径，无需令牌）
     */
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(authService.createCaptcha());
    }

    /**
     * 登录（放行路径，无需令牌）
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<SysUser> getCurrentUser() {
        return Result.success(authService.getCurrentUser());
    }

    /**
     * 修改密码
     */
    @OperLog("修改密码")
    @PutMapping("/password")
    public Result<Map<String, Object>> changePassword(@Valid @RequestBody PasswordDTO dto) {
        authService.changePassword(dto);
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        return Result.success("密码修改成功", data);
    }

    /**
     * 安全退出：拉黑当前令牌（需登录，防止匿名请求滥用）
     */
    @PostMapping("/logout")
    public Result<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader.substring(7));
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        return Result.success("退出成功", data);
    }
}
