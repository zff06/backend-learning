package top.zff06.security.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证与授权失败都要往响应里写 JSON，写法收在一处
 */
public final class RestErrorWriter {

    private RestErrorWriter() {
    }

    public static void write(HttpServletResponse response, int httpStatus, int code, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }

    /** 未认证 401 */
    public static void unauthorized(HttpServletResponse response) throws IOException {
        write(response, 401, 401, "未认证或令牌失效");
    }

    /** 已认证但无权访问 403 */
    public static void forbidden(HttpServletResponse response) throws IOException {
        write(response, 403, 403, "已认证但无权访问");
    }
}
