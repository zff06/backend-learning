package top.mqxu.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 密码登录入参。用 record：四个字段都是不可变的，也没有默认值要补。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password) {
}
