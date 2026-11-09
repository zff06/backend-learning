package top.mqxu.security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.mqxu.security.common.Result;
import top.mqxu.security.dto.UserInfo;

import java.util.Map;

/**
 * 第 ⑤ 条链下的业务接口。路径规则写在 SecurityConfig 里，这里只关心业务本身。
 * <p>
 * 加了两处 @PreAuthorize 用于演示方法级鉴权——它抛的 AuthorizationDeniedException
 * 走不到 JsonAccessDeniedHandler，由 GlobalExceptionHandler 接住返回 403。
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    /** 只要求登录。任何拿到正确 token 的人都能进。 */
    @GetMapping("/hello")
    public Result<Map<String, Object>> hello() {
        return Result.ok(Map.of(
                "message", "你好，" + CurrentUser.username(),
                "authorities", CurrentUser.authorities()));
    }

    @GetMapping("/me")
    public Result<UserInfo> me() {
        return Result.ok(new UserInfo(CurrentUser.username(), CurrentUser.authorities()));
    }

    /** 公开接口，链上配了 permitAll，不带 token 也能访问。 */
    @GetMapping("/public/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }

    /** 要求 ADMIN 角色，规则在链上（hasRole）。alice 这里会 403。 */
    @GetMapping("/admin/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(Map.of(
                "message", "欢迎回来，管理员 " + CurrentUser.username(),
                "authorities", CurrentUser.authorities()));
    }

    /**
     * 删文章：要求 ARTICLE_DELETE 权限点，规则也在链上。
     * 注意 bob 是管理员但没有这个权限点，同样 403——这是角色与权限点的区别。
     */
    @DeleteMapping("/articles/{id}")
    public Result<String> deleteArticle(@PathVariable Long id) {
        return Result.ok("已删除文章 " + id);
    }

    /** 方法级鉴权：只有同时具备 ADMIN 与 AUDITOR 才能进（bob 可以，carol 不行）。 */
    @GetMapping("/admin/audit")
    @PreAuthorize("hasRole('ADMIN') and hasRole('AUDITOR')")
    public Result<String> audit() {
        return Result.ok("审计日志已导出");
    }
}
