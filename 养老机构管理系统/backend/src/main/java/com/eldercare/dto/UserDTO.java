package com.eldercare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户新增/修改请求参数（修改时不传密码）
 */
@Data
public class UserDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "admin|nurse|family", message = "角色只能是 admin/nurse/family")
    private String role;

    private String phone;

    /** 状态：1 启用 / 0 禁用 */
    private Integer status;
}