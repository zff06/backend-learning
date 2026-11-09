package top.mqxu.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 过滤器链的 403：认出来了，但这条规则不放行。
 * <p>
 * 注意它接不到 @PreAuthorize 抛的 AuthorizationDeniedException——
 * 那个异常在 Controller 调用栈里抛，压根走不到 ExceptionTranslationFilter。
 * Controller 层的那份实现在 GlobalExceptionHandler 里。
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"code":403,"message":"没有访问权限","data":null}""");
    }
}
