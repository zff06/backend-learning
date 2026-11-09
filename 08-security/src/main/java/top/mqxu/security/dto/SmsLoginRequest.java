package top.mqxu.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 短信登录入参。
 * <p>
 * 手机号格式在 DTO 上校验，验证码本身由 SmsCodeAuthenticationProvider 校验——
 * 后者要从 Redis 里比对，属于认证逻辑，不该散到入参校验里。
 */
public record SmsLoginRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "验证码不能为空")
        String code) {
}
