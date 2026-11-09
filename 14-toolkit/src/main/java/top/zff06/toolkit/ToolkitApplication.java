package top.zff06.toolkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description ToolkitApplication
 **/
@SpringBootApplication
public class ToolkitApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(ToolkitApplication.class, args);
    }
}
