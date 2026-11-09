package top.mqxu.security.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.mqxu.security.common.ErrorCode;
import top.mqxu.security.common.Result;
import top.mqxu.security.config.SecurityProperties;
import top.mqxu.security.dto.LoginResponse;
import top.mqxu.security.dto.SmsLoginRequest;
import top.mqxu.security.security.JwtService;
import top.mqxu.security.sms.SmsCodeAuthenticationToken;
import top.mqxu.security.sms.SmsCodeService;
import top.mqxu.security.sms.SmsSendTooFrequentException;

import java.util.List;

/**
 * 短信登录：两步都很薄。
 * <p>
 * 发码只负责生成验证码；登录把 (手机号, 验证码) 包成 SmsCodeAuthenticationToken 交给
 * AuthenticationManager，剩下的交给 Provider——和密码登录走完全同一条路，只是凭证类型不同。
 * <p>
 * 类上的 @Validated 不能省：@RequestParam 上的 @Pattern 靠它才生效，漏了校验不会执行。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth/sms")
@RequiredArgsConstructor
public class SmsAuthController {

    private final SmsCodeService smsCodeService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SecurityProperties properties;

    @PostMapping("/code")
    public ResponseEntity<Result<String>> sendCode(
            @RequestParam
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone) {
        String code;
        try {
            code = smsCodeService.sendCode(phone);
        } catch (SmsSendTooFrequentException e) {
            return ResponseEntity.badRequest()
                    .body(Result.fail(ErrorCode.SMS_SEND_TOO_FREQUENT.getCode(), e.getMessage()));
        }

        String message = properties.getSms().isExposeCode()
                ? "验证码已发送（演示模式，验证码：" + code + "）"
                : "验证码已发送";
        return ResponseEntity.ok(Result.ok(message));
    }

    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@Valid @RequestBody SmsLoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new SmsCodeAuthenticationToken(request.phone(), request.code()));
        } catch (AuthenticationException e) {
            return translate(e);
        }

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = jwtService.generate(authentication.getName(), authorities);

        // 注意：短信登录进来的用户没有 FACTOR_PASSWORD，
        // 可以据此限制"必须用密码登录才能改密码/绑手机号"
        LoginResponse body = new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpiresInSeconds(),
                authentication.getName(),
                authorities);

        log.info("短信登录成功 user={} authorities={}", authentication.getName(), authorities);
        return ResponseEntity.ok(Result.ok(body));
    }

    /**
     * Provider 自己写的中文消息（"验证码错误或已过期"）保留原样，只补状态码。
     */
    private ResponseEntity<Result<LoginResponse>> translate(AuthenticationException e) {
        HttpStatus status = e instanceof DisabledException
                || e instanceof LockedException
                || e instanceof AccountExpiredException
                || e instanceof CredentialsExpiredException
                ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;

        int code = e instanceof DisabledException ? ErrorCode.ACCOUNT_DISABLED.getCode()
                : e instanceof LockedException ? ErrorCode.ACCOUNT_LOCKED.getCode()
                : e instanceof AccountExpiredException ? ErrorCode.ACCOUNT_EXPIRED.getCode()
                : e instanceof CredentialsExpiredException ? ErrorCode.CREDENTIALS_EXPIRED.getCode()
                : e instanceof BadCredentialsException ? ErrorCode.SMS_CODE_ERROR.getCode()
                : ErrorCode.UNAUTHORIZED.getCode();

        String message = e instanceof DisabledException ? "账号已被禁用"
                : e instanceof LockedException ? "账号已被锁定"
                : e instanceof AccountExpiredException ? "账号已过期"
                : e instanceof CredentialsExpiredException ? "登录凭证已过期"
                : e.getMessage();

        log.warn("短信登录失败：{}", message);
        return ResponseEntity.status(status)
                .body(new Result<>(code, message, null));
    }
}
