package top.mqxu.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 启动类放在 top.mqxu.security 下，九个包都在它的扫描范围里。
 * <p>
 * 不写 @MapperScan：MyBatis-Plus 的 starter 会自动扫描 @Mapper 标记的接口。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }
}
