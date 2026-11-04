package top.zff06.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import top.zff06.security.common.BusinessException;
import top.zff06.security.common.ErrorCode;
import top.zff06.security.dto.LoginRequest;
import top.zff06.security.security.JwtTokenService;
import top.zff06.security.security.SecurityUser;
import top.zff06.security.vo.LoginResponse;

/**
 * 登录与当前用户
 * 密码校验交给 DaoAuthenticationProvider（AuthenticationManager）：
 * BadCredentialsException 统一转 30001，不区分"密码错"和"用户不存在"
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException | DisabledException e) {
            // DisabledException 单独转成 30002：账号停用是明确知道原因的失败
            if (e instanceof DisabledException) {
                throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
            }
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        String token = tokenService.generateToken(user.getUsername(), user.getNickname());
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresAt(tokenService.expiresAt())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roles(user.getRoles())
                .build();
    }
}
