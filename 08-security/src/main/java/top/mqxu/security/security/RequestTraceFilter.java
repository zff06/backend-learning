package top.mqxu.security.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求追踪：打印"谁访问了什么、耗时多久"。
 * <p>
 * 位置很关键——必须挂在 AuthorizationFilter 之前，此时 SecurityContext 里已经有真实身份，
 * 才能读到用户名。挂得比认证过滤器还早，读到的永远是 anonymous，日志就没有意义了。
 * <p>
 * 不加 @Component：容器里的 Filter bean 会被 Spring Boot 自动注册到 Servlet 容器，
 * 那样它会跑在 Security 过滤器链之外，读到的 SecurityContext 永远是空的。
 * 这个类只作为普通对象在 SecurityConfig 里 new 出来塞进链里。
 */
@Slf4j
public class RequestTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String user = authentication == null || !authentication.isAuthenticated()
                    ? "anonymous" : authentication.getName();
            log.info("{} {} -> {} | user={} | {}ms",
                    request.getMethod(), request.getRequestURI(),
                    response.getStatus(), user, System.currentTimeMillis() - start);
        }
    }
}
