package top.mqxu.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 过滤器链的 401。
 * <p>
 * 不用默认实现的原因有两条：
 * <ol>
 *   <li>默认实现按 Accept 协商：浏览器请求 302 跳登录页，前端拿到的是 HTML；</li>
 *   <li>API 请求会带 WWW-Authenticate 头，浏览器看到就弹原生账号密码框，
 *       把"未登录"变成一个用户看不懂的对话框。</li>
 * </ol>
 * 所以这里既固定返回 JSON，也刻意不写 WWW-Authenticate。
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"code":401,"message":"未登录或登录已过期","data":null}""");
    }
}
