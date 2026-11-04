package top.zff06.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @auther zff06
 * @data 2026/10/28
 * @description LogController
 **/
@Slf4j
@RestController
@RequestMapping("/api/log")
public class LogController {

    /**
     * 带参数的接口，用于验证过滤器生效
     */
    @GetMapping("/hello")
    public String hello(@RequestParam("name") String name,
                        @RequestParam(value = "age", required = false) Integer age) {
        log.info("[LogController] 处理请求，name={}, age={}", name, age);
        return "hello, " + name + (age != null ? ", " + age + " 岁" : "");
    }
}
