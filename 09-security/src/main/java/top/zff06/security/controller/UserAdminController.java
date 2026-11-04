package top.zff06.security.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.security.common.Result;
import top.zff06.security.dto.CreateUserRequest;
import top.zff06.security.security.SecurityUser;
import top.zff06.security.service.UserService;
import top.zff06.security.vo.UserProfile;

import java.util.List;

/**
 * 账号管理接口：URL 规则与方法注解两道防线，字符串形态保持一致（都用 ROLE_ADMIN）
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserService userService;

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('user:read')")
    public Result<List<UserProfile>> list() {
        return Result.ok(userService.list());
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('user:write')")
    public Result<UserProfile> create(@Valid @RequestBody CreateUserRequest request) {
        return Result.ok(userService.create(request));
    }

    @DeleteMapping("/users/{username}")
    public Result<Void> delete(@AuthenticationPrincipal SecurityUser user,
                               @PathVariable String username) {
        userService.delete(user.getUsername(), username);
        return Result.ok();
    }
}
