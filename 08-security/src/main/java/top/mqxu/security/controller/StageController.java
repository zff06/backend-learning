package top.mqxu.security.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import top.mqxu.security.common.Result;
import top.mqxu.security.dto.UserInfo;

import java.util.Map;

/**
 * stage1 / stage2 / stage3 的业务接口。
 * <p>
 * 这三个 Controller 里没有一行鉴权代码——规则全写在 SecurityConfig 的链上。
 * 这正是分链演示的目的：同一个接口，走哪条链就受哪套规则约束。
 */
@RestController
public class StageController {

    // ---------- stage1：全拦截，只支持 HTTP Basic ----------

    @GetMapping("/stage1/hello")
    public Result<String> stage1Hello() {
        return Result.ok("stage1 全拦截通过：" + CurrentUser.username());
    }

    // ---------- stage2：白名单，GET 公开 / DELETE 要登录 ----------

    @GetMapping("/stage2/public/ping")
    public Result<String> stage2Public() {
        return Result.ok("stage2 公开接口，不需要登录");
    }

    @GetMapping("/stage2/articles/{id}")
    public Result<Map<String, Object>> stage2Read(@PathVariable Long id) {
        return Result.ok(Map.of("id", id, "title", "文章 " + id, "visitor", CurrentUser.username()));
    }

    @DeleteMapping("/stage2/articles/{id}")
    public Result<String> stage2Delete(@PathVariable Long id) {
        return Result.ok("stage2 删除成功：" + id + " by " + CurrentUser.username());
    }

    // ---------- stage3：角色与权限点 ----------

    @GetMapping("/stage3/admin/settings")
    public Result<String> stage3Admin() {
        return Result.ok("hasRole('ADMIN') 通过：" + CurrentUser.username());
    }

    @GetMapping("/stage3/ops/monitor")
    public Result<String> stage3Ops() {
        return Result.ok("hasAnyRole('ADMIN','AUDITOR') 通过：" + CurrentUser.username());
    }

    @GetMapping("/stage3/audit/logs")
    public Result<String> stage3Audit() {
        return Result.ok("hasAllRoles('ADMIN','AUDITOR') 通过：" + CurrentUser.username());
    }

    @DeleteMapping("/stage3/articles/{id}")
    public Result<String> stage3Delete(@PathVariable Long id) {
        return Result.ok("hasAuthority('ARTICLE_DELETE') 通过：" + id);
    }

    // ---------- stage4：表单登录后的页面 ----------

    /**
     * 登录成功后跳这里。返回 JSON 而不是 HTML，方便 curl 与测试断言；
     * 要看页面的场景直接访问 /stage4/login.html。
     */
    @GetMapping("/stage4/me")
    public Result<UserInfo> stage4Me() {
        return Result.ok(new UserInfo(CurrentUser.username(), CurrentUser.authorities()));
    }

    /**
     * 读 CSRF token。页面的隐藏域用它，前端也可以先请求这里再提交表单。
     * CsrfToken 作为参数注入时由 Security 填充，不会返回 null。
     */
    @GetMapping("/stage4/csrf")
    public Result<String> csrfToken(org.springframework.security.web.csrf.CsrfToken token) {
        return Result.ok(token.getToken());
    }

    @PostMapping("/stage4/logout")
    public Result<Void> stage4Logout() {
        // 真正注销由 Security 的 LogoutFilter 处理，请求到这里说明已经注销过了
        return Result.ok();
    }
}
