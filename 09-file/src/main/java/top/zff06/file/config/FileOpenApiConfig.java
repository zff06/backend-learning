package top.zff06.file.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 练习 1：给已有模块（09-file）补文档的元信息配置
 * <p>
 * 只加了 springdoc 依赖和注解，OSS 的业务代码一行没改。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description FileOpenApiConfig
 **/
@Configuration
public class FileOpenApiConfig {

    @Bean
    public OpenAPI fileOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("文件上传服务接口文档")
                        .version("1.0.0")
                        .description("""
                                阿里云 OSS 上传接口。

                                **调用约定**
                                1. 请求类型是 multipart/form-data，表单字段名见各接口的 Parameter 说明
                                2. 单文件上限 5MB，单请求合计上限 20MB（application.yml 里 spring.servlet.multipart）
                                3. 返回体是纯文本 URL，不是统一响应体 Result，这是模块 09 的历史设计
                                """));
    }
}
