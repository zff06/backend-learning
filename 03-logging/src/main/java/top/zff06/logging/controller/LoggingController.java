package top.zff06.logging.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.logging.common.LogMaskUtil;

/**
 * @auther zff06
 * @data 2026/9/23
 * @description LoggingController
 **/
@RestController
@RequestMapping("/logging")
@Slf4j
public class LoggingController {
    @GetMapping("/hello")
    public String hello(){
        return "日志管理 模块";
    }
    @GetMapping("/demo")
    public String demo(){
        // 五个日志级别，从低到高：trace < debug < info < warn < error
        // dev 环境已临时把 root 级别调到 DEBUG（见 logback-spring.xml）
        log.trace("这是 trace 日志，需要 trace 级别才会输出");
        log.debug("这是 debug 日志，dev 环境已调高到 DEBUG，可以看到");
        log.info("处理 /logging/demo 请求");
        log.warn("这是一条警告日志");
        log.error("这是一条错误日志");
        // 占位符 {} 写法：推荐！避免字符串拼接，只有日志真的要输出时才拼接
        String username = "zff06";
        int status = 200;
        log.info("用户 {} 登录成功，状态码：{}", username, status);
        log.info("用户手机号：{}", LogMaskUtil.maskPhone("13812345678"));
        return "ok";
    }
}
