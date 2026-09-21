package top.zff06.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author zff06
 * @date 2026/9/9
 * @description 启动类，必须放在所有业务包的最外层（根包）
 **/
@SpringBootApplication
public class ConfigApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class,args);
    }
}
