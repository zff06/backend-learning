package top.zff06.doc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档元信息与分组
 * <p>
 * 一条链三个消费者：/v3/api-docs（JSON，给 Apifox/Postman 导入）、/swagger-ui.html（人看）、CI 契约测试。
 * 三者读的是同一份 JSON，所以注解写对一次就够了。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description OpenApiConfig
 **/
@Configuration
public class OpenApiConfig {

    /**
     * 文档元信息 + 安全方案（练习 2）
     * <p>
     * 安全方案登记在 components 里，再在接口上用 @SecurityRequirement(name = "bearer") 引用；
     * 登记之后 Swagger UI 右上角才会出现 Authorize 按钮。
     */
    @Bean
    public OpenAPI docOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("图书服务接口文档")
                        .version("1.0.0")
                        .description("""
                                模块 10 接口文档综合练习。

                                **调用约定**
                                1. 所有接口返回统一响应体 `Result<T>`，业务成功看 `code == 200`
                                2. 需要登录的接口请在右上角 Authorize 里填入 token（不带 `Bearer ` 前缀）
                                3. 文档地址仅开发环境开放，生产环境关闭
                                """)
                        .contact(new Contact().name("zff06").email("2395006165@qq.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录接口拿到的 JWT，直接粘 token 本体")));
        // 注意：这里不再 addSecurityItem 全局声明。
        // 全局声明会让所有接口（包括公开的查询接口）在 OpenAPI 里都带上 security，
        // 调用方据此生成代码就会要求处处传 token。鉴权按接口粒度用 @SecurityRequirement 标。
    }

    /**
     * 分组写法一：按路径前缀切
     * <p>
     * paths-to-match 写错（比如 /book/** 少个 s）分组文档就是空的，且不报错，只能靠测试兜住。
     */
    @Bean
    public GroupedOpenApi bookGroup() {
        return GroupedOpenApi.builder()
                .group("01-图书管理")
                .pathsToMatch("/books/**")
                .build();
    }

    /**
     * 分组写法二：按包切
     * <p>
     * 按包切只认 controller 所在的包，新增 controller 不用改配置；按路径切的好处是能跨包聚合业务。
     */
    @Bean
    public GroupedOpenApi systemGroup() {
        return GroupedOpenApi.builder()
                .group("02-系统运维")
                .packagesToScan("top.zff06.doc.controller.system")
                .build();
    }
}
