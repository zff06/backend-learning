package top.zff06.actuator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模块 12：应用监控（Actuator + Micrometer + Prometheus）启动类。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description ActuatorApplication
 **/
@SpringBootApplication
public class ActuatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActuatorApplication.class, args);
    }
}
