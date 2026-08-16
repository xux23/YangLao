package com.eldercare.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.dto.UserDTO;
import com.eldercare.entity.SysUser;

/**
 * 用户管理业务接口（仅管理员）
 */
public interface UserService {

    /**
     * 用户分页查询，支持用户名模糊、角色筛选
     */
    Page<SysUser> pageUser(int page, int size, String username, String role);

    /**
     * 新增用户
     */
    void addUser(UserDTO dto);

    /**
     * 修改用户（用户名不可修改）
     */
    void updateUser(Long id, UserDTO dto);

    /**
     * 删除用户（不能删除自己）
     */
    void deleteUser(Long id);

    /**
     * 重置密码为默认密码 123456
     */
    void resetPassword(Long id);
}