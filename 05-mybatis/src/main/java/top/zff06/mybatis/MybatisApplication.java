package top.zff06.mybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @auther zff06
 * @data 2026/9/23
 * @description MybatisApplication
 **/
@SpringBootApplication
@MapperScan(basePackages = "top.zff06.mybatis.mapper")
public class MybatisApplication {
    public static void main(String[] args) {
        SpringApplication.run(MybatisApplication.class,args);
    }
}
