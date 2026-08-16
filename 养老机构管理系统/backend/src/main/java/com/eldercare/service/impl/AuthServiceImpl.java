package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eldercare.common.BusinessException;
import com.eldercare.dto.PasswordDTO;
import com.eldercare.entity.SysUser;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.JwtUtil;
import com.eldercare.security.UserContext;
import com.eldercare.service.AuthService;
import com.eldercare.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证业务实现：登录签发 JWT、获取当前用户、修改密码
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public LoginVO login(String username, String password) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        // 用户名或密码错误统一提示，不暴露账号是否存在
        if (user == null || !encoder.matches(password, user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user);
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
}