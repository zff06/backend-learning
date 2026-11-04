package top.zff06.security.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.security.common.Result;
import top.zff06.security.dto.LoginRequest;
import top.zff06.security.security.SecurityUser;
import top.zff06.security.service.AuthService;
import top.zff06.security.vo.LoginResponse;
import top.zff06.security.vo.UserProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证接口：登录放行，me 需要已认证
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserProfile> me(@AuthenticationPrincipal SecurityUser user) {
        List<String> authorities = new ArrayList<>();
        user.getAuthorities().forEach(a -> authorities.add(a.getAuthority()));
        UserProfile profile = UserProfile.builder()
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roles(user.getRoles())
                .authorities(authorities)
                .enabled(user.isEnabled())
                .build();
        return Result.ok(profile);
    }
}
