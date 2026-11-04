package top.zff06.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @auther zff06
 * @data 2026/10/28
 * @description LogFilter
 **/
@Slf4j
@Component
public class LogFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String method = req.getMethod();
        String uri = req.getRequestURI();
        // 获取所有请求参数（键值对）
        Map<String, String[]> params = req.getParameterMap();
        log.info("[LogFilter] 请求方法：{}，URI：{}，请求时间：{}", method, uri, LocalDateTime.now());
        params.forEach((name, values) -> log.info("[LogFilter] 参数 {} = {}", name, String.join(",", values)));
        chain.doFilter(request,response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException{
        log.info("LogFilter 过滤器初始化");
    }

    @Override
    public void destroy(){
        log.info("LogFilter 过滤器销毁");
    }

}
