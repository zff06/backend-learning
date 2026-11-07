package top.zff06.actuator.info;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用信息贡献者：往 /actuator/info 里塞模块信息与负责人信息。
 *
 * <p>用途：线上多实例时，curl 一下就知道这个 IP 跑的是哪个版本、哪个 profile。
 * 注意 startedAt 用的是请求时刻的 LocalDateTime.now()，属于简化处理，
 * 真实启动时间应该用 ManagementFactory.getRuntimeMXBean().getStartTime()。
 *
 * @auther zff06
 * @data 2026/11/7
 * @description AppInfoContributor
 **/
@Component
@RequiredArgsConstructor
public class AppInfoContributor implements InfoContributor {

    private final Environment environment;

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> module = new LinkedHashMap<>();
        module.put("name", environment.getProperty("spring.application.name", "unknown"));
        module.put("profiles", String.join(",", environment.getActiveProfiles()));
        module.put("port", environment.getProperty("server.port"));
        module.put("startedAt", LocalDateTime.now().toString());

        Map<String, Object> owner = new LinkedHashMap<>();
        owner.put("team", "backend-learning");
        owner.put("contact", "mqxu@example.com");

        builder.withDetail("module", module).withDetail("owner", owner);
    }
}
