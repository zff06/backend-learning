package top.mqxu.security.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 从 Authorization 头解析 JWT，把身份写进 SecurityContext。
 * <p>
 * 三个设计决定（6.4 节）：
 * <ol>
 *   <li>验签失败不返回 401，只清空上下文继续放行——由后面的 AuthorizationFilter 统一拒绝，
 *       "未登录"的错误响应只有 JsonAuthenticationEntryPoint 一份实现；</li>
 *   <li>新建 context 再整体 setContext，避免 ThreadLocal 在线程复用时串数据；</li>
 *   <li>没有 token 直接放行，不打断匿名请求。</li>
 * </ol>
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = jwtService.parseAuthorizationHeader(request.getHeader(HttpHeaders.AUTHORIZATION));

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parse(token);
            List<?> raw = claims.get(JwtService.CLAIM_AUTHORITIES, List.class);
            List<GrantedAuthority> authorities = raw == null ? List.of()
                    : raw.stream().map(String::valueOf)
                    .<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();

            Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                    claims.getSubject(), null, authorities);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            log.debug("JWT 认证通过：{} authorities={}", claims.getSubject(), authorities);
        } catch (JwtException | IllegalArgumentException e) {
            // 过期、篡改、结构不对，一律当成"没带凭证"，交给授权层拒绝
            SecurityContextHolder.clearContext();
            log.debug("JWT 解析失败，按匿名处理：{}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
