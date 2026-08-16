package com.eldercare.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eldercare.aspect.OperLog;
import com.eldercare.common.Result;
import com.eldercare.dto.UserDTO;
import com.eldercare.entity.SysUser;
import com.eldercare.security.RequireRole;
import com.eldercare.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口（仅管理员）
 */
@RestController
@RequestMapping("/api/users")
@RequireRole("admin")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户分页查询，支持用户名模糊、角色筛选
     */
    @GetMapping
    public Result<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        return Result.success(userService.pageUser(page, size, username, role));
    }

    /**
     * 新增用户
     */
    @OperLog("新增用户")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody UserDTO dto) {
        userService.addUser(dto);
        return Result.success("新增成功", null);
    }

    /**
     * 修改用户
     */
    @OperLog("修改用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        userService.updateUser(id, dto);
        return Result.success("修改成功", null);
    }

    /**
     * 删除用户
     */
    @OperLog("删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功", null);
    }

    /**
     * 重置密码为默认密码 123456
     */
    @OperLog("重置密码")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success("密码已重置为 123456", null);
    }
}