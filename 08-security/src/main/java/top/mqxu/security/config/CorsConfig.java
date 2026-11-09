package top.mqxu.security.config;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * CORS 配置。
 * <p>
 * 关键点：CORS 必须挂在 Security 上（链上加 .cors(withDefaults())，让它找到这个 bean），
 * 只写 WebMvcConfigurer.addCorsMappings 或 Controller 上的 @CrossOrigin 不够。
 * <p>
 * 原因：Security 的过滤器链在最外层，请求在 AuthorizationFilter 就被 401 拦下了，
 * 根本走不到 MVC，401 响应上也就没有 CORS 头。浏览器看到没有 Access-Control-Allow-Origin，
 * 会把整个响应丢掉，前端只看到 TypeError: Failed to fetch——连"401"都看不到。
 */
@Configuration
public class CorsConfig {

    /**
     * 这里没有直接把 SecurityProperties 当方法参数注入，而是自己从 Environment 绑一次。
     * 原因：@Configuration 的处理早于 @ConfigurationProperties 的绑定，
     * 直接用参数注入在某些加载顺序下会拿到还没填好值的对象。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(Environment environment) {
        List<String> origins = Binder.get(environment)
                .bind("mqxu.security.cors.allowed-origins", Bindable.listOf(String.class))
                .orElse(List.of("http://localhost:5173"));

        CorsConfiguration config = new CorsConfiguration();
        // 用 allowedOriginPatterns 而不是 allowedOrigins：前者支持 http://localhost:*，
        // 且响应里回显具体来源；后者不支持通配，写 * 时也不能和 allowCredentials(true) 一起用
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type",
                "X-Requested-With", "X-XSRF-TOKEN"));
        // 不写这一行，JS 读不到响应头里的 Authorization 与 Content-Disposition（下载文件名）
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 只对 /api/** 生效：stage1-4 是教学用的同源页面，不需要跨域
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
