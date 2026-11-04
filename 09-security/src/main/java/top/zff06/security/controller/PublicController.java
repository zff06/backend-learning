package top.zff06.security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.security.common.Result;

import java.util.Map;

/**
 * 放行的健康检查接口
 */
@RestController
@RequestMapping("/public")
public class PublicController {

    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        return Result.ok(Map.of("status", "ok"));
    }
}
