package top.zff06.config.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @auther zff06
 * @data 2026/9/9
 * @description ConfigController
 **/
@RestController
@RequestMapping("/config")
public class ConfigController {
    @Value("${server.port}")
    private Integer serverPort;
    @Value("${spring.application.name}")
    private String appName;
    @Value("${zff06.name}")
    private String myName;
    @Value("${zff06.job}")
    private String myJob;

    @GetMapping("/basic")
    public String getBasicInfo(){
        return "服务器端口是："+this.serverPort+",应用名称是："+appName;
    }
    @GetMapping("/my")
    public String getMyInfo(){
        return "我的姓名是："+this.myName+",职业是："+myJob;
    }

}
