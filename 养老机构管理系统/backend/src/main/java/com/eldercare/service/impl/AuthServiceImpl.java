package com.eldercare.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.BusinessException;
import com.eldercare.common.RedisKeys;
import com.eldercare.common.RedisService;
import com.eldercare.dto.LoginDTO;
import com.eldercare.dto.PasswordDTO;
import com.eldercare.entity.SysUser;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.JwtUtil;
import com.eldercare.security.UserContext;
import com.eldercare.service.AuthService;
import com.eldercare.vo.CaptchaVO;
import com.eldercare.vo.LoginVO;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 认证业务实现：验证码签发、登录（锁定 + 验证码 + BCrypt 校验 + 签发 JWT）、
 * 获取当前用户、修改密码、安全退出（令牌黑名单）。
 * <p>
 * Redis key 设计（均为短生命周期数据，TTL 自动过期，无需清理任务）：
 * <ul>
 *   <li>captcha:{captchaId}      —— 验证码文本，5 分钟</li>
 *   <li>login:fail:{username}    —— 密码错误计数，首次失败起 15 分钟</li>
 *   <li>jwt:blacklist:{token}    —— 登出黑名单，TTL = 令牌剩余有效期</li>
 * </ul>
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisService redisService;

    @Value("${app.captcha.enabled}")
    private boolean captchaEnabled;

    @Value("${app.captcha.expire-seconds}")
    private long captchaExpireSeconds;

    @Value("${app.login.max-fail-count}")
    private int maxFailCount;

    @Value("${app.login.lock-minutes}")
    private long lockMinutes;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public CaptchaVO createCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 44, 4, 12);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = captcha.getCode();
        redisService.set(RedisKeys.captchaKey(captchaId), code.toLowerCase(),
                Duration.ofSeconds(captchaExpireSeconds));
        return new CaptchaVO(captchaId, captcha.getImageBase64Data());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // ① 锁定检查：失败次数达到上限直接拒绝（放在最前，锁定期间不再消耗验证码）
        String failKey = RedisKeys.failKey(dto.getUsername());        String failCount = redisService.get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= maxFailCount) {
            throw new BusinessException(400, "密码错误次数过多，账号已被锁定，请" + lockMinutes + "分钟后再试");
        }
        // ② 验证码校验：无论成败都立即删除，保证一次性（防重放）
        if (captchaEnabled) {
            checkCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        }
        // ③ 账号密码校验
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        // 用户名或密码错误统一提示，不暴露账号是否存在
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            redisService.increment(failKey, Duration.ofMinutes(lockMinutes));
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        // 登录成功清除失败计数
        redisService.delete(failKey);
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user);
    }

    /**
     * 验证码校验：未输入/已过期/不匹配均拒绝
     */
    private void checkCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            throw new BusinessException(400, "请输入验证码");
        }
        String key = RedisKeys.captchaKey(captchaId);
        String code = redisService.get(key);
        redisService.delete(key);
        if (code == null) {
            throw new BusinessException(400, "验证码已过期，请刷新后重试");
        }
        if (!code.equals(captchaCode.trim().toLowerCase())) {
            throw new BusinessException(400, "验证码错误");
        }
    }

    @Override
    public SysUser getCurrentUser() {
        Long userId = UserContext.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "登录状态已失效，请重新登录");
        }
        return user;
    }

    @Override
    public void changePassword(PasswordDTO dto) {
        SysUser user = getCurrentUser();
        // 验证原密码
        if (!encoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void logout(String token) {
        Claims claims = jwtUtil.parseToken(token);
        long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
        // 剩余有效期内拉黑令牌；已过期则无需处理
        if (remainingMillis > 0) {
            redisService.set(RedisKeys.blacklistKey(token), "1", Duration.ofMillis(remainingMillis));
        }
    }
}
