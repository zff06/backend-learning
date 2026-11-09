package top.mqxu.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import top.mqxu.security.sms.SmsCodeService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 五条链的真实行为，14 个用例（4 登录 + 6 第五链 + 3 前三链 + 1 表单与短信）。
 * <p>
 * 用 TestRestTemplate 起真实端口，而不是 MockMvc：这样过滤器链、CORS、错误响应体
 * 全都真的走一遍。MockMvc 绕过一部分 Servlet 容器行为，验证不到 401 的响应格式。
 * <p>
 * SmsCodeService 换成 mock：验证码的存取逻辑已由 InMemorySmsCodeTest 覆盖，
 * 这里只关心"短信登录这条路能不能通"，同时让测试不依赖 Redis 是否在跑。
 * <p>
 * 注意数据库是必需的：schema.sql 与 data.sql 在启动时执行，六个账号就在里面。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private TestRestTemplate rest;

    @MockitoBean
    private SmsCodeService smsCodeService;

    @BeforeEach
    void stubSmsCode() {
        when(smsCodeService.sendCode(anyString())).thenReturn("123456");
        when(smsCodeService.verify(anyString(), anyString()))
                .thenAnswer(inv -> "123456".equals(inv.getArgument(1)));
    }

    // ---------------------------------------------------------------- 工具

    /** Basic 头手工拼：最接近真实客户端，也不依赖容器里的 AuthenticationManager 配置 */
    private String basic(String username) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    }

    private HttpHeaders json() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    private HttpHeaders basicHeader(String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, basic(username));
        return headers;
    }

    private ResponseEntity<Map<String, Object>> post(String url, String body) {
        return rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, json()), mapOf());
    }

    private ResponseEntity<Map<String, Object>> get(String url, HttpHeaders headers) {
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), mapOf());
    }

    private ResponseEntity<Map<String, Object>> delete(String url, HttpHeaders headers) {
        return rest.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), mapOf());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<Map<String, Object>> mapOf() {
        return (Class) Map.class;
    }

    @SuppressWarnings("unchecked")
    private String login(String username) {
        ResponseEntity<Map<String, Object>> response = post("/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return (String) data.get("token");
    }

    private void assertError(ResponseEntity<?> response, HttpStatus status, int code, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo(code);
        assertThat(body.get("message")).isEqualTo(message);
    }

    // ---------------------------------------------------------------- 登录（4）

    @Test
    @DisplayName("1. 密码正确返回 token、有效期与权限列表")
    @SuppressWarnings("unchecked")
    void loginReturnsToken() {
        ResponseEntity<Map<String, Object>> response = post("/api/auth/login",
                "{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat((String) data.get("token")).hasSizeGreaterThan(50);
        assertThat(data.get("tokenType")).isEqualTo("Bearer");
        assertThat(data.get("username")).isEqualTo("alice");
        assertThat(((Number) data.get("expiresIn")).longValue()).isEqualTo(7200);

        // 不按位置断言：sec_role 没有 ORDER BY，InnoDB 返回顺序不保证
        assertThat((List<String>) data.get("authorities"))
                .contains("ROLE_USER", "ARTICLE_DELETE", "FACTOR_PASSWORD");
    }

    @Test
    @DisplayName("2. 密码错误返回 401，消息是中文")
    void wrongPasswordIs401() {
        assertError(post("/api/auth/login", "{\"username\":\"alice\",\"password\":\"wrong\"}"),
                HttpStatus.UNAUTHORIZED, 401, "用户名或密码错误");
    }

    @Test
    @DisplayName("3. 禁用账号 403、锁定账号 403，消息不同")
    void disabledAndLockedAre403() {
        assertError(post("/api/auth/login",
                        "{\"username\":\"dave\",\"password\":\"" + PASSWORD + "\"}"),
                HttpStatus.FORBIDDEN, 403, "账号已被禁用");

        assertError(post("/api/auth/login",
                        "{\"username\":\"frank\",\"password\":\"" + PASSWORD + "\"}"),
                HttpStatus.FORBIDDEN, 403, "账号已被锁定");
    }

    @Test
    @DisplayName("4. 入参为空返回 400，不是 500 也不是 401")
    void blankUsernameIs400() {
        ResponseEntity<Map<String, Object>> response =
                post("/api/auth/login", "{\"username\":\"\",\"password\":\"\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo(400);
    }

    @Test
    @DisplayName("4b. @RequestParam 上的 @Pattern 失败也是 400，不是 500")
    void invalidPhoneIs400() {
        // 没有 @ExceptionHandler(ConstraintViolationException) 的话，
        // 手机号格式错会变成"服务器内部错误"，排查时很容易误判成服务挂了
        assertError(post("/api/auth/sms/code?phone=123", null),
                HttpStatus.BAD_REQUEST, 400, "手机号格式不正确");
    }

    // ---------------------------------------------------------------- 第 ⑤ 条链（6）

    @Test
    @DisplayName("5. 不带 token 访问 /api/hello 返回过滤器链的 401，且没有 WWW-Authenticate")
    void anonymousApiIs401() {
        ResponseEntity<Map<String, Object>> response = get("/api/hello", new HttpHeaders());

        assertError(response, HttpStatus.UNAUTHORIZED, 401, "未登录或登录已过期");
        // 写了 WWW-Authenticate，浏览器就会弹原生登录框
        assertThat(response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isNull();
    }

    @Test
    @DisplayName("6. 带 token 能访问 /api/hello，用户名正确")
    void tokenGrantsAccess() {
        ResponseEntity<Map<String, Object>> response = get("/api/hello", bearer(login("alice")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data.get("message")).isEqualTo("你好，alice");
    }

    @Test
    @DisplayName("7. 篡改签名后 token 失效，回落到 401")
    void tamperedTokenIs401() {
        String token = login("alice");
        // 改签名段第一个字符，不要改最后一个（可能只动到 Base64url 的填充位）
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart)
                + (original == 'A' ? 'B' : 'A')
                + token.substring(signatureStart + 1);

        assertThat(get("/api/hello", bearer(tampered)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("8. hasRole('ADMIN')：alice 403，bob 200")
    void adminRoleIsEnforced() {
        assertError(get("/api/admin/dashboard", bearer(login("alice"))),
                HttpStatus.FORBIDDEN, 403, "没有访问权限");

        assertThat(get("/api/admin/dashboard", bearer(login("bob"))).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("9. hasAuthority('ARTICLE_DELETE')：alice 200，管理员 bob 反而 403")
    void articleDeleteNeedsPermission() {
        assertThat(delete("/api/articles/1001", bearer(login("alice"))).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(delete("/api/articles/1001", bearer(login("bob"))).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("10. @PreAuthorize 拒绝时由 Controller 层返回真实 403")
    void preAuthorizeDenialReturnsJson403() {
        // bob 同时有 ADMIN 与 AUDITOR，放行
        assertThat(get("/api/admin/audit", bearer(login("bob"))).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // carol 只有 ADMIN。这个异常走不到 JsonAccessDeniedHandler，
        // 由 GlobalExceptionHandler 接住，否则会被兜底成 500
        assertError(get("/api/admin/audit", bearer(login("carol"))),
                HttpStatus.FORBIDDEN, 403, "没有访问权限");
    }

    // ---------------------------------------------------------------- 第 ①-③ 条链（3）

    @Test
    @DisplayName("11. stage1 全拦截：无凭证 401，Basic 通过")
    void stage1RequiresBasic() {
        assertThat(get("/stage1/hello", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/stage1/hello", basicHeader("alice")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("12. stage2 同一路径：GET 放行，DELETE 要登录")
    void stage2MatchesHttpMethod() {
        assertThat(get("/stage2/articles/1", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(delete("/stage2/articles/1", new HttpHeaders()).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("13. stage3 四种规则：hasRole / hasAnyRole / hasAllRoles / hasAuthority")
    void stage3Rules() {
        // hasAllRoles：carol 只有 ADMIN → 403；bob 两个都有 → 200
        assertThat(get("/stage3/audit/logs", basicHeader("carol")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/stage3/audit/logs", basicHeader("bob")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        // hasAnyRole：erin 只有 AUDITOR，也能进
        assertThat(get("/stage3/ops/monitor", basicHeader("erin")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        // hasAuthority：alice 有 ARTICLE_DELETE → 200；bob 没有 → 403
        assertThat(delete("/stage3/articles/1", basicHeader("alice")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(delete("/stage3/articles/1", basicHeader("bob")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------------------------------------------------------------- 第 ④ 条链与短信（1）

    @Test
    @DisplayName("14. stage4 登录页可匿名访问；短信发码 → 换 token；未登录访问受保护资源 401")
    @SuppressWarnings("unchecked")
    void stage4AndSmsLogin() {
        // stage4：登录页是 HTML，不能用 Map 接收
        ResponseEntity<String> page = rest.getForEntity("/stage4/login.html", String.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getBody()).contains("stage4/login");

        // stage4：未登录访问受保护接口，返回 401 JSON 而不是 302 跳登录页
        assertError(get("/stage4/me", new HttpHeaders()),
                HttpStatus.UNAUTHORIZED, 401, "未登录或登录已过期");

        // 短信：发码（演示模式回显验证码），再用验证码换 token
        ResponseEntity<Map<String, Object>> code = post("/api/auth/sms/code?phone=13900001111", null);
        assertThat(code.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) code.getBody().get("data")).contains("123456");

        ResponseEntity<Map<String, Object>> logged = post("/api/auth/sms/login",
                "{\"phone\":\"13900001111\",\"code\":\"123456\"}");
        assertThat(logged.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = (Map<String, Object>) logged.getBody().get("data");
        assertThat((String) data.get("token")).isNotBlank();

        // 短信登录没有 FACTOR_PASSWORD：只有密码校验那条路才会追加这个认证因子
        assertThat((List<String>) data.get("authorities"))
                .contains("ROLE_USER")
                .doesNotContain("FACTOR_PASSWORD");

        assertError(post("/api/auth/sms/login",
                        "{\"phone\":\"13900001111\",\"code\":\"000000\"}"),
                HttpStatus.UNAUTHORIZED, 30010, "验证码错误或已过期");
    }
}
