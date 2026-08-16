package com.eldercare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.common.BusinessException;
import com.eldercare.dto.UserDTO;
import com.eldercare.entity.ElderInfo;
import com.eldercare.entity.SysUser;
import com.eldercare.mapper.ElderInfoMapper;
import com.eldercare.mapper.SysUserMapper;
import com.eldercare.security.UserContext;
import com.eldercare.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户管理业务实现（仅管理员可调用）
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ElderInfoMapper elderInfoMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 默认密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public Page<SysUser> pageUser(int page, int size, String username, String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(username), SysUser::getUsername, username)
                .eq(StringUtils.hasText(role), SysUser::getRole, role)
                .orderByDesc(SysUser::getId);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void addUser(UserDTO dto) {
        if (userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setId(null);
        // 密码未填时使用默认密码 123456
        user.setPassword(encoder.encode(
                StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : DEFAULT_PASSWORD));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
    }

    @Override
    public void updateUser(Long id, UserDTO dto) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 只修改姓名、角色、手机号、状态，用户名不可修改
        user.setRealName(dto.getRealName());
        user.setRole(dto.getRole());
        user.setPhone(dto.getPhone());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (id.equals(UserContext.getUserId())) {
            throw new BusinessException(400, "不能删除当前登录账号");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 删除的是家属账号时，同步解除其与老人的关联
        if ("family".equals(user.getRole())) {
            ElderInfo elder = elderInfoMapper.selectOne(
                    new LambdaQueryWrapper<ElderInfo>().eq(ElderInfo::getFamilyId, id));
            if (elder != null) {
                elder.setFamilyId(null);
                elderInfoMapper.updateById(elder);
            }
        }
        userMapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(encoder.encode(DEFAULT_PASSWORD));
        userMapper.updateById(user);
    }
}