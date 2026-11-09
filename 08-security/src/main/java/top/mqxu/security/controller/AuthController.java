package top.mqxu.security.controller;

import jakarta.validation.Valid;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.mqxu.security.common.ErrorCode;
import top.mqxu.security.common.Result;
import top.mqxu.security.dto.LoginRequest;
import top.mqxu.security.dto.LoginResponse;
import top.mqxu.security.security.JwtService;

import java.util.List;
import java.util.Map;

/**
 * 密码登录。
 * <p>
 * 不自己查库比密码，而是把凭证交给 AuthenticationManager——
 * 这样密码校验、账号状态检查、权限装配全部复用框架实现，
 * 后面加 @PreAuthorize、加认证因子都不用改这个类。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<Result<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(), request.password()));
        } catch (AuthenticationException e) {
            // 密码错和账号不可用要分开：前者是"你输错了"，后者是"你该去找管理员"。
            // 混成一个 401，用户会一直重试密码。
            return translate(e);
        }

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String token = jwtService.generate(authentication.getName(), authorities);

        LoginResponse body = new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpiresInSeconds(),
                authentication.getName(),
                authorities);

        log.info("登录成功 user={} authorities={}", authentication.getName(), authorities);
        return ResponseEntity.ok(Result.ok(body));
    }

    /**
     * 默认消息是英文（Bad credentials / User is disabled），直接透给前端用户看不懂，换成中文。
     * 返回码也按错误类型分开：密码错 401，账号状态问题 403。
     */
    private ResponseEntity<Result<LoginResponse>> translate(AuthenticationException e) {
        if (e instanceof DisabledException) {
            return error(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED);
        }
        if (e instanceof LockedException) {
            return error(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_LOCKED);
        }
        if (e instanceof AccountExpiredException) {
            return error(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_EXPIRED);
        }
        if (e instanceof CredentialsExpiredException) {
            return error(HttpStatus.FORBIDDEN, ErrorCode.CREDENTIALS_EXPIRED);
        }
        if (e instanceof BadCredentialsException) {
            return error(HttpStatus.UNAUTHORIZED, ErrorCode.BAD_CREDENTIALS);
        }
        log.warn("登录失败：{}", e.getMessage());
        return error(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
    }

    private ResponseEntity<Result<LoginResponse>> error(HttpStatus status, ErrorCode errorCode) {
        Result<LoginResponse> body = new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 给前端确认 token 里到底装了什么，不用自己解 Base64。
     */
    @PostMapping("/decode")
    public Result<Map<String, Object>> decode(@RequestBody Map<String, String> body) {
        var claims = jwtService.parse(body.get("token"));
        return Result.ok(Map.of(
                "subject", claims.getSubject(),
                "authorities", claims.get(JwtService.CLAIM_AUTHORITIES, List.class),
                "issuedAt", String.valueOf(claims.getIssuedAt()),
                "expiration", String.valueOf(claims.getExpiration())));
    }
}
