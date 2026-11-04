package top.zff06.web.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.zff06.web.filter.TimeStatInterceptor;

/**
 * @auther zff06
 * @data 2026/10/28
 * @description WebConfig
 **/
@Configuration
@AllArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final TimeStatInterceptor timeStatInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(timeStatInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/static/**","/error")
                .order(1);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET","POST","PUT","DELETE")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
