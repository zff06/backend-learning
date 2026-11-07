package top.zff06.doc.controller.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.doc.common.Result;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统运维接口
 * <p>
 * 单独放一个包：分组"02-系统运维"按包扫描（packagesToScan）就靠这个位置，
 * 从这里能看出"按包切"和"按路径切"的区别 —— 本控制器的路径前缀不是 /books。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description SystemController
 **/
@Tag(name = "系统运维", description = "服务自身状态，按包分组")
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final Environment environment;

    @Operation(summary = "服务健康摘要", description = "返回当前 profile、启动时间等基础信息")
    @SecurityRequirement(name = "bearer")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("profiles", String.join(",", environment.getActiveProfiles()).isEmpty()
                ? "default" : String.join(",", environment.getActiveProfiles()));
        data.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return Result.ok(data);
    }
}
